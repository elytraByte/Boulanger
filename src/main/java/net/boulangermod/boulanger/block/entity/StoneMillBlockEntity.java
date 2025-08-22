package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class StoneMillBlockEntity extends AbstractPoweredBlockEntity
        implements AbstractProcessingBlock.Tickable, MenuProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    /* ───────────── slots ───────────── */
    private static final int SLOT_IN0 = 0;
    private static final int SLOT_IN1 = 1;
    private static final int SLOT_IN2 = 2;
    private static final int SLOT_OUT = 3;

    /* ───────────── tuning ───────────── */
    private static final int MAX_MILL_TIME    = 200;  // ticks per operation
    private static final int FE_COST_PER_TICK = 5;    // FE/t while milling

    // Internal battery
    private static final int FE_CAPACITY    = FE_COST_PER_TICK * 500;
    private static final int FE_MAX_RECEIVE = FE_COST_PER_TICK * 40;
    private static final int STARVE_GRACE_TICKS = 2;

    private static final int PROGRESS_PARTICLE_PERIOD = 20; // ticks between small puffs

    // ── NEW: manual turning (number of clicks per craft) ───────────────────────
    private static final int HAND_TURNS_PER_OP = 12;
    private static final int MANUAL_TURN_TICKS = Math.max(1, MAX_MILL_TIME / HAND_TURNS_PER_OP);

    // UI lamp state
    private boolean lampOn = false;

    // Milling state
    private int  millProgress = 0;
    private boolean milling   = false;
    private int starvedTicks  = 0;

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

    @Override public BlockEntityType<?> getType() { return ModBlockEntities.STONE_MILL_BE.get(); }

    /* ───────────── helpers ───────────── */

    private static ItemStack buildFlourStack(FlourItemType type) {
        ItemStack s = new ItemStack(ModItems.FLOUR_ITEM.get(), 1);
        s.set(ModDataComponentTypes.FLOUR_TYPE.get(), type.toFlourType());
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.getModelIndex()));
        return s;
    }

    private boolean isWheat(ItemStack s) { return !s.isEmpty() && s.is(Items.WHEAT); }

    private int countInputs() {
        int c = 0;
        if (isWheat(itemHandler.getStackInSlot(SLOT_IN0))) c++;
        if (isWheat(itemHandler.getStackInSlot(SLOT_IN1))) c++;
        if (isWheat(itemHandler.getStackInSlot(SLOT_IN2))) c++;
        return c;
    }

    private int outputSpaceFor(ItemStack target) {
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUT);
        if (out.isEmpty()) return target.getMaxStackSize();
        if (!ItemStack.isSameItemSameComponents(out, target)) return 0;
        return out.getMaxStackSize() - out.getCount();
    }

    /** How many items we can craft *right now* (0..3) */
    private int craftableNow() {
        ItemStack target = buildFlourStack(FlourItemType.WHOLE_WHEAT_FLOUR);
        int inputs = countInputs();
        if (inputs <= 0) return 0;
        int space  = outputSpaceFor(target);
        return Math.min(inputs, Math.max(0, space));
    }

    private boolean canMill() { return craftableNow() > 0; }

    private void craftResultAndEffects(Level level, BlockPos pos, BlockState state) {
        int craftable = craftableNow();
        if (craftable <= 0) return;

        int produced = 0;
        int[] ins = {SLOT_IN0, SLOT_IN1, SLOT_IN2};
        for (int idx : ins) {
            if (produced >= craftable) break;
            ItemStack in = itemHandler.getStackInSlot(idx);
            if (isWheat(in)) {
                in.shrink(1);
                produced++;
            }
        }
        if (produced <= 0) return;

        ItemStack result1 = buildFlourStack(FlourItemType.WHOLE_WHEAT_FLOUR);
        ItemStack out     = itemHandler.getStackInSlot(SLOT_OUT);

        if (out.isEmpty()) {
            result1.setCount(produced);
            itemHandler.setStackInSlot(SLOT_OUT, result1);
        } else if (ItemStack.isSameItemSameComponents(out, result1)) {
            out.grow(produced);
        }

        float vol   = 0.9f + level.random.nextFloat() * 0.1f;
        float pitch = 0.9f + level.random.nextFloat() * 0.2f;
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, vol, pitch);
        spawnMillParticles(level, pos, state, 8);

        dbg("Crafted {}x WHOLE_WHEAT_FLOUR (out now {}), inputs consumed across up to 3 slots",
                produced, itemHandler.getStackInSlot(SLOT_OUT).getCount());
    }

    private void spawnMillParticles(Level level, BlockPos pos, BlockState state, int count) {
        if (!(level instanceof ServerLevel sl)) return;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.02;
        double cz = pos.getZ() + 0.5;
        sl.sendParticles(ParticleTypes.CLOUD, cx, cy, cz, count, 0.12, 0.03, 0.12, 0.02);
    }

    public void resetMilling() {
        if (millProgress != 0 || milling) {
            dbg("Reset milling (progress was {}, milling={}, starvedTicks={})",
                    millProgress, milling, starvedTicks);
        }
        millProgress = 0;
        milling = false;
        starvedTicks = 0;
        setChanged();
    }

    public int  getMixProgress()       { return millProgress; } // kept for UI
    public boolean isMilling()         { return milling; }
    public static int getMaxMixTime()  { return MAX_MILL_TIME; }

    /* ───────────── NEW: manual turning API ───────────── */
    /**
     * Advance the mill by a hand turn. Returns true if we actually progressed
     * (i.e., had inputs + output space), false otherwise.
     */
    public boolean manualTurnByPlayer(@Nullable Player player) {
        Level level = getLevel();
        if (level == null) return false;

        if (!canMill()) {
            // light “thunk” feedback when turning with no work to do
            level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_OFF,
                    SoundSource.BLOCKS, 0.25f, 0.8f + level.random.nextFloat() * 0.2f);
            return false;
        }

        // Show progress on UI while hand-cranking too
        milling = true;
        starvedTicks = 0;

        int before = millProgress;
        millProgress = Math.min(MAX_MILL_TIME, millProgress + MANUAL_TURN_TICKS);

        // light sound + dust for each turn
        level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE,
                SoundSource.BLOCKS, 0.45f, 1.0f + level.random.nextFloat() * 0.1f);
        spawnMillParticles(level, worldPosition, getBlockState(), 2);

        boolean finished = (millProgress >= MAX_MILL_TIME);
        if (finished) {
            if (canMill()) craftResultAndEffects(level, worldPosition, getBlockState());
            resetMilling();
        }

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

        dbg("Manual turn by {}, progress {}→{}, finished={}",
                (player != null ? player.getGameProfile().getName() : "unknown"),
                before, millProgress, finished);
        return true;
    }

    /* ───────────── persistence ───────────── */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MillProgress", millProgress);
        tag.putBoolean("Milling", milling);
        tag.putInt("StarvedTicks", starvedTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        millProgress = tag.getInt("MillProgress");
        milling      = tag.getBoolean("Milling");
        starvedTicks = tag.getInt("StarvedTicks");
        dbg("Loaded state: progress={}, milling={}, energy={}/{}",
                millProgress, milling, getEnergyStored(), getEnergyCapacity());
    }

    /* ───────────── UI / menu ───────────── */
    @Override
    public Component getDisplayName() { return Component.translatable("stone_mill.boulanger"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new net.boulangermod.boulanger.screen.StoneMillBlockMenu(id, inv, this);
    }

    /* ───────────── ticking (RF path) ───────────── */
    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        serverEnergyTick();

        boolean newLamp = getEnergyStored() > 0;
        if (newLamp != lampOn) {
            lampOn = newLamp;
            dirty = true;
            dbg("Lamp {} (energy={}/{})", lampOn ? "ON" : "OFF", getEnergyStored(), getEnergyCapacity());
        }

        if (!milling) {
            if (canMill() && hasPowerForTick()) {
                milling = true;
                starvedTicks = 0;
                dirty = true;
                dbg("Started milling (energy={}/{}, craftableNow={})",
                        getEnergyStored(), getEnergyCapacity(), craftableNow());
            }
        }

        if (milling) {
            if (tryConsumePowerForTick()) {
                millProgress++;

                if (millProgress > 0 && (millProgress % PROGRESS_PARTICLE_PERIOD) == 0) {
                    spawnMillParticles(level, pos, state, 3);
                }

                starvedTicks = 0;
                if (millProgress >= MAX_MILL_TIME) {
                    if (canMill()) craftResultAndEffects(level, pos, state);
                    resetMilling();
                    dirty = true;
                }
            } else {
                starvedTicks++;
                if (starvedTicks > STARVE_GRACE_TICKS) {
                    milling = false;
                    dirty = true;
                    dbg("Paused milling after {} starved ticks (energy={})",
                            STARVE_GRACE_TICKS, getEnergyStored());
                } else {
                    dbg("Starved tick {}/{} (energy={})", starvedTicks, STARVE_GRACE_TICKS, getEnergyStored());
                }
            }
        }

        if (dirty) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /* ───────────── logging helper ───────────── */
    private void dbg(String fmt, Object... args) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("[StoneMill] " + fmt, args);
    }
}
