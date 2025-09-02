package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.WheatBushelItem;
import net.boulangermod.boulanger.item.WheatVariety;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StoneMillBlockEntity extends AbstractPoweredBlockEntity
        implements AbstractProcessingBlock.Tickable, MenuProvider {

    /* ───────────── slots ───────────── */
    private static final int SLOT_IN0 = 0;
    private static final int SLOT_IN1 = 1;
    private static final int SLOT_IN2 = 2;
    private static final int SLOT_OUT = 3;

    /* ───────────── tuning ───────────── */
    private static final int MAX_MILL_TIME    = 200;  // ticks per op (UI ref)
    private static final int FE_COST_PER_TICK = 10;

    // Internal battery
    private static final int FE_CAPACITY    = FE_COST_PER_TICK * 500;
    private static final int FE_MAX_RECEIVE = FE_COST_PER_TICK * 40;

    // Manual turning config
    private static final int HAND_TURNS_PER_OP = 12; // 12 hits -> 1 craft
    private static final int MANUAL_TURN_TICKS = Math.max(1, MAX_MILL_TIME / HAND_TURNS_PER_OP);

    // Shared hit model
    private static final int HITS_PER_CRAFT = 12;

    // RF cost per virtual hit
    private static final int RF_PER_HIT = 10;

    // Powered speed: multiplier vs manual (1.0 = same speed; 1.25 ≈ 25% faster)
    private static final double POWER_SPEED_MULT = 1.25;
    private static final int POWER_TURN_TICKS =
            Math.max(1, (int)Math.round(MANUAL_TURN_TICKS / POWER_SPEED_MULT));

    // SFX
    private static final int POWER_LOOP_SOUND_PERIOD_TICKS = 6;

    // Manual timeout (~1 minute)
    private static final int MANUAL_TIMEOUT_TICKS = 20 * 60;

    // UI lamp state
    private boolean lampOn = false;

    // Milling state (UI progress bar uses millProgress/MAX_MILL_TIME)
    private int  millProgress = 0;
    private boolean milling   = false;
    private int hitProgress   = 0;        // 0..HITS_PER_CRAFT-1

    // Track RF vs manual and manual idle time
    private boolean rfMilling = false;
    private long lastManualTurnGameTime = 0L;

    // RF cadence: perform one virtual hand-hit every POWER_TURN_TICKS
    private int poweredHitCooldown = 0;

    public StoneMillBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.STONE_MILL_BE.get(),
                pos, state,
                /*slots*/ 4,
                /*energyCapacity*/ FE_CAPACITY,
                /*maxReceive*/     FE_MAX_RECEIVE,
                /*energyPerTick*/  FE_COST_PER_TICK
        );
    }

    /** Screen uses this to draw the “on” bulb. True if we currently hold any energy. */
    public boolean isGridPowered() { return getEnergyStored() > 0; }

    /* ───────────── helpers ───────────── */

    private static ItemStack buildFlourStack(FlourItemType type) {
        ItemStack s = new ItemStack(ModItems.FLOUR_ITEM.get(), 1);
        s.set(ModDataComponentTypes.FLOUR_TYPE.get(), type.toFlourType());
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.getModelIndex()));
        return s;
    }

    @Nullable
    private FlourItemType flourTypeForInput(ItemStack in) {
        if (in == null || in.isEmpty()) return null;

        // Vanilla wheat -> whole wheat flour
        if (in.is(Items.WHEAT)) {
            return FlourItemType.WHOLE_WHEAT_FLOUR;
        }

        // Wheat Bushels -> look at variety (component or class hint)
        WheatVariety variety = in.get(ModDataComponentTypes.WHEAT_VARIETY.get());
        if (variety != null || in.getItem() instanceof WheatBushelItem) {
            if (variety == WheatVariety.DURUM) {
                // Durum -> semolina
                return FlourItemType.SEMOLINA_FLOUR;
            }
            // All other varieties -> whole wheat
            return FlourItemType.WHOLE_WHEAT_FLOUR;
        }

        return null; // not a millable grain
    }

    private boolean isMillableGrain(ItemStack s) { return flourTypeForInput(s) != null; }

    /** Count how many inputs match a given flour output type. */
    private int countInputsForType(FlourItemType type) {
        int c = 0;
        if (type == flourTypeForInput(itemHandler.getStackInSlot(SLOT_IN0))) c++;
        if (type == flourTypeForInput(itemHandler.getStackInSlot(SLOT_IN1))) c++;
        if (type == flourTypeForInput(itemHandler.getStackInSlot(SLOT_IN2))) c++;
        return c;
    }

    /** Decide which flour type to craft this tick. Prefer the OUT slot's type, else first valid input. */
    @Nullable
    private FlourItemType chooseOutputType() {
        // If OUT slot has flour, stick with that type so we stack correctly
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUT);
        if (!out.isEmpty()) {
            FlourType outFt = out.get(ModDataComponentTypes.FLOUR_TYPE.get());
            if (outFt != null) {
                for (FlourItemType it : FlourItemType.values()) {
                    if (it.toFlourType().type().equals(outFt.type())) {
                        return it;
                    }
                }
            }
        }
        // Otherwise pick the first millable input’s mapped type
        FlourItemType t;
        if ((t = flourTypeForInput(itemHandler.getStackInSlot(SLOT_IN0))) != null) return t;
        if ((t = flourTypeForInput(itemHandler.getStackInSlot(SLOT_IN1))) != null) return t;
        if ((t = flourTypeForInput(itemHandler.getStackInSlot(SLOT_IN2))) != null) return t;
        return null;
    }

    private int outputSpaceFor(ItemStack target) {
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUT);
        if (out.isEmpty()) return target.getMaxStackSize();
        if (!ItemStack.isSameItemSameComponents(out, target)) return 0;
        return out.getMaxStackSize() - out.getCount();
    }

    /** How many items we can craft *right now* (0..3) */
    private int craftableNow() {
        FlourItemType chosen = chooseOutputType();
        if (chosen == null) return 0;

        ItemStack target = buildFlourStack(chosen);
        int inputs = countInputsForType(chosen);
        if (inputs <= 0) return 0;

        int outCount = itemHandler.getStackInSlot(SLOT_OUT).getCount();
        int space = outputSpaceFor(target);
        space = Math.min(space, 64 - (itemHandler.getStackInSlot(SLOT_OUT).isEmpty() ? 0 : outCount));
        return Math.min(inputs, Math.max(0, space));
    }

    private boolean canMill() { return craftableNow() > 0; }

    /** One hit (manual or powered): small particle; on 12th hit, craft + burst. */
    private void doSingleHit(Level level, BlockPos pos, BlockState state) {
        spawnMillParticles(level, pos, state, 1);

        hitProgress++;
        millProgress = Math.min(MAX_MILL_TIME, millProgress + MANUAL_TURN_TICKS);

        if (hitProgress >= HITS_PER_CRAFT) {
            hitProgress = 0;

            if (canMill()) {
                craftResultAndEffects(level, pos, state); // includes final sound + burst
            }
            resetMilling();            // clear progress after craft
            if (canMill()) milling = true; // keep armed if more work remains
        }
    }

    /** Powered work: perform one virtual hand-hit every POWER_TURN_TICKS; loop sound while working. */
    private void poweredWorkTick(Level level, BlockPos pos, BlockState state) {
        if (craftableNow() <= 0) return;

        poweredHitCooldown++;

        // Only attempt a hit when we've waited long enough to match desired powered cadence
        if (poweredHitCooldown < POWER_TURN_TICKS) {
            // gentle loop sound so it feels alive
            if (level.getGameTime() % POWER_LOOP_SOUND_PERIOD_TICKS == 0) {
                float vol   = 0.25f + level.random.nextFloat() * 0.05f;
                float pitch = 0.95f + level.random.nextFloat() * 0.10f;
                level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, vol, pitch);
            }
            return;
        }

        // Ready to perform one hit: need enough energy for this hit
        if (getEnergyStored() < RF_PER_HIT) {
            // RF starved: immediately reset progress and stop RF milling
            resetMilling();
            return;
        }

        // Drain RF_PER_HIT in FE_COST_PER_TICK chunks
        int need = RF_PER_HIT;
        int chunks = (need + FE_COST_PER_TICK - 1) / FE_COST_PER_TICK;
        for (int c = 0; c < chunks; c++) {
            if (!tryConsumePowerForTick()) {
                resetMilling();
                return;
            }
        }

        // Do one virtual hand-hit (1 particle); craft on the 12th
        doSingleHit(level, pos, state);
        poweredHitCooldown = 0;

        // Looping gentle grind sound while actively working (after the hit too)
        if (level.getGameTime() % POWER_LOOP_SOUND_PERIOD_TICKS == 0) {
            float vol   = 0.35f + level.random.nextFloat() * 0.05f;
            float pitch = 0.95f + level.random.nextFloat() * 0.10f;
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, vol, pitch);
        }
    }

    private void craftResultAndEffects(Level level, BlockPos pos, BlockState state) {
        FlourItemType chosen = chooseOutputType();
        if (chosen == null) return;

        ItemStack proto = buildFlourStack(chosen);
        ItemStack out   = itemHandler.getStackInSlot(SLOT_OUT);

        int hardCap = Math.min(proto.getMaxStackSize(), 64);

        int space;
        if (out.isEmpty()) {
            space = hardCap;
        } else if (!ItemStack.isSameItemSameComponents(out, proto)) {
            return;
        } else {
            space = hardCap - out.getCount();
            if (space <= 0) return;
        }

        int craftable = Math.min(craftableNow(), space);
        if (craftable <= 0) return;

        int produced = 0;
        for (int idx : new int[]{SLOT_IN0, SLOT_IN1, SLOT_IN2}) {
            if (produced >= craftable) break;
            ItemStack in = itemHandler.getStackInSlot(idx);
            if (chosen == flourTypeForInput(in)) {
                in.shrink(1);
                produced++;
            }
        }
        if (produced <= 0) return;

        if (out.isEmpty()) {
            proto.setCount(produced);
            itemHandler.setStackInSlot(SLOT_OUT, proto);
        } else {
            out.grow(produced);
        }

        float vol   = 0.9f + level.random.nextFloat() * 0.1f;
        float pitch = 0.9f + level.random.nextFloat() * 0.2f;
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, vol, pitch);
        spawnMillParticles(level, pos, state, 8);
    }

    private void spawnMillParticles(Level level, BlockPos pos, BlockState state, int count) {
        if (!(level instanceof ServerLevel sl)) return;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.02;
        double cz = pos.getZ() + 0.5;
        sl.sendParticles(ParticleTypes.CLOUD, cx, cy, cz, count, 0.12, 0.03, 0.12, 0.02);
    }

    public void resetMilling() {
        millProgress = 0;
        hitProgress = 0;
        poweredHitCooldown = 0;
        milling = false;
        rfMilling = false;
        setChanged();
    }

    public int  getMixProgress()       { return millProgress; } // kept for UI
    public boolean isMilling()         { return milling; }
    public static int getMaxMixTime()  { return MAX_MILL_TIME; }

    /* ───────────── manual turning API ───────────── */
    public boolean manualTurnByPlayer(@Nullable Player player) {
        Level level = getLevel();
        if (level == null) return false;

        if (!canMill()) {
            level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_OFF,
                    SoundSource.BLOCKS, 0.25f, 0.8f + level.random.nextFloat() * 0.2f);
            return false;
        }

        milling = true;
        // DO NOT disable RF here; allow RF to resume immediately if power exists
        lastManualTurnGameTime = level.getGameTime();

        level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE,
                SoundSource.BLOCKS, 0.45f, 1.0f + level.random.nextFloat() * 0.1f);

        doSingleHit(level, worldPosition, getBlockState());

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }

    /* ───────────── persistence ───────────── */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MillProgress", millProgress);
        tag.putBoolean("Milling", milling);
        tag.putInt("HitProgress", hitProgress);
        tag.putBoolean("RFMilling", rfMilling);
        tag.putLong("LastManualTurn", lastManualTurnGameTime);
        tag.putInt("PoweredHitCooldown", poweredHitCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        millProgress = tag.getInt("MillProgress");
        milling      = tag.getBoolean("Milling");
        hitProgress  = tag.getInt("HitProgress");
        rfMilling    = tag.getBoolean("RFMilling");
        lastManualTurnGameTime = tag.getLong("LastManualTurn");
        poweredHitCooldown = tag.getInt("PoweredHitCooldown");
    }

    /* ───────────── UI / menu ───────────── */
    @Override
    public Component getDisplayName() { return Component.translatable("stone_mill.boulanger"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new net.boulangermod.boulanger.screen.StoneMillBlockMenu(id, inv, this);
    }

    /* ───────────── ticking (RF + manual timeout) ───────────── */
    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        serverEnergyTick();

        boolean newLamp = getEnergyStored() > 0;
        if (newLamp != lampOn) { lampOn = newLamp; dirty = true; }

        boolean poweredAvailable = getEnergyStored() >= RF_PER_HIT;
        boolean haveWork = canMill();

        // ✅ Re-arm/maintain RF milling whenever we have power + items,
        // even if the player recently hand-turned (fixes the “stops until you reinsert items” bug)
        if (poweredAvailable && haveWork) {
            if (!milling) { milling = true; dirty = true; }
            if (!rfMilling) { rfMilling = true; dirty = true; }
        }

        if (milling && rfMilling) {
            if (poweredAvailable) {
                int before = millProgress;
                poweredWorkTick(level, pos, state);
                if (!canMill()) { resetMilling(); dirty = true; }
                else if (millProgress != before) dirty = true;
            } else {
                // RF starved: immediately reset progress to zero and stop RF milling
                resetMilling();
                dirty = true;
            }
        }

        // Manual timeout: only applies when not RF-driven
        if (milling && !rfMilling) {
            long idle = level.getGameTime() - lastManualTurnGameTime;
            if (idle >= MANUAL_TIMEOUT_TICKS) {
                resetMilling();
                dirty = true;
            }
        }

        if (dirty) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
}
