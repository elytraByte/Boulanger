package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.screen.MixingBlockMenu;
import net.boulangermod.boulanger.util.IngredientStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

public class MixingBlockEntity extends AbstractProcessingBlockEntity
        implements AbstractProcessingBlock.Tickable {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int INPUT_BOWL   = 0;
    public static final int OUTPUT_BOWL  = 1;
    public static final int OUTPUT_DOUGH = 2;

    // 20 kg default for basic mixer
    private static final double MAX_DOUGH_WEIGHT_GRAMS = 22680.0;
    private static final int MAX_MIX_TIME = 100;

    private final MixerState mixer = new MixerState();
    private boolean mixing = false;
    private int mixProgress = 0;

    public MixingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXING_BLOCK_BE.get(), pos, state, 3);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────────
    public List<IngredientStack> getIngredientList() { return mixer.getIngredientList(); }
    public int getMixProgress() { return mixProgress; }
    public boolean isMixing() { return mixing; }
    public static int getMaxMixTime() { return MAX_MIX_TIME; }

    /** Invoked by StartMixingPacket handler. */
    public void startMixing() {
        if (mixer.getIngredientList().isEmpty()) {
            LOGGER.warn("No ingredients! Nothing to mix.");
            return;
        }
        mixing = true;
        mixProgress = 0;
        syncToClient();
    }

    // ── Tick (server) ────────────────────────────────────────────────────────────
    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide) return;

        // Intake: consume ALL filled bowls in the input stack, add their contents, return the same count of empty bowls
        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && MixerState.isWeighedIngredient(in)) {
            int count = in.getCount();
            // Add the ingredient 'count' times (each bowl is identical; stacked items share components)
            for (int i = 0; i < count; i++) {
                mixer.addIngredientFromBowl(in);
            }

            // Clear input and spawn exactly 'count' empty bowls
            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);
            spawnEmptyBowls(count);
            syncToClient();
        }

        // Mixing progress
        if (mixing) {
            mixProgress++;
            if (mixProgress >= MAX_MIX_TIME) {
                ItemStack dough = mixer.generateDough(level, MAX_DOUGH_WEIGHT_GRAMS);
                if (!dough.isEmpty()) {
                    itemHandler.setStackInSlot(OUTPUT_DOUGH, dough);
                }
                mixing = false;
                mixProgress = 0;
                syncToClient();
            }
        }
    }

    // Spawn N empty bowls into OUTPUT_BOWL, respecting slot limits and dropping overflow to the world.
    private void spawnEmptyBowls(int quantity) {
        if (quantity <= 0) return;

        ItemStack out = itemHandler.getStackInSlot(OUTPUT_BOWL);
        final int slotLimit = Math.min(64, itemHandler.getSlotLimit(OUTPUT_BOWL));
        int remaining = quantity;

        if (out.isEmpty() || out.getItem() != Items.BOWL) {
            int toPlace = Math.min(remaining, slotLimit);
            itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL, toPlace));
            remaining -= toPlace;
        } else {
            int free = Math.max(0, slotLimit - out.getCount());
            int toAdd = Math.min(remaining, free);
            if (toAdd > 0) {
                out.grow(toAdd);
                itemHandler.setStackInSlot(OUTPUT_BOWL, out);
                remaining -= toAdd;
            }
        }

        // Drop any overflow bowls at the block position
        if (remaining > 0 && level != null && !level.isClientSide) {
            while (remaining > 0) {
                int drop = Math.min(64, remaining);
                net.minecraft.world.level.block.Block.popResource(level, worldPosition, new ItemStack(Items.BOWL, drop));
                remaining -= drop;
            }
        }
    }

    public void clearAndEject(@Nullable Player player) {
        // stop mixing
        this.mixing = false;
        this.mixProgress = 0;

        // give back contents of slots 0..2
        for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
            ItemStack st = this.itemHandler.getStackInSlot(slot);
            if (st.isEmpty()) continue;

            if (player instanceof net.minecraft.server.level.ServerPlayer sp
                    && sp.getInventory().add(st.copy())) {
                // successfully added to player, clear slot
                this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            } else if (this.level != null && !this.level.isClientSide) {
                net.minecraft.world.level.block.Block.popResource(this.level, this.worldPosition, st.copy());
                this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }

        // clear internal ingredients/state
        try {
            // if your MixerState has a clear() helper, use it:
            this.mixer.clear();
        } catch (Throwable t) {
            // fallback if clear() doesn't exist: nothing to do here; but ideally add mixer.clear()
        }

        // sync
        if (this.level != null && !this.level.isClientSide) {
            setChanged();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── Persistence ──────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("Mixing", mixing);
        tag.putInt("MixProgress", mixProgress);
        tag.put("Ingredients", mixer.saveIngredientList());
        tag.put("PreciseTotalsG", mixer.savePreciseTotals());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        mixing = tag.getBoolean("Mixing");
        mixProgress = tag.getInt("MixProgress");
        mixer.loadIngredientList(tag.getList("Ingredients", ListTag.TAG_COMPOUND));
        if (tag.contains("PreciseTotalsG")) {
            mixer.loadPreciseTotals(tag.getCompound("PreciseTotalsG"));
        } else {
            mixer.loadPreciseTotals(null);
        }
    }

    // ── Menu / name ──────────────────────────────────────────────────────────────
    @Override public Component getDisplayName() {
        return Component.translatable("mixing_block.boulanger");
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new MixingBlockMenu(id, inv, this);
    }

    // ── Networking / sync ────────────────────────────────────────────────────────
    /** Mark dirty and push an update packet so the client sees changes. */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider regs) {
        handleUpdateTag(pkt.getTag(), regs);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        tag.putBoolean("Mixing", mixing);
        tag.putInt("MixProgress", mixProgress);
        tag.put("Ingredients", mixer.saveIngredientList());
        tag.put("PreciseTotalsG", mixer.savePreciseTotals());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider regs) {
        super.handleUpdateTag(tag, regs);
        mixing = tag.getBoolean("Mixing");
        mixProgress = tag.getInt("MixProgress");
        if (tag.contains("Ingredients")) {
            mixer.loadIngredientList(tag.getList("Ingredients", ListTag.TAG_COMPOUND));
        }
        if (tag.contains("PreciseTotalsG")) {
            mixer.loadPreciseTotals(tag.getCompound("PreciseTotalsG"));
        } else {
            mixer.loadPreciseTotals(null);
        }
    }
}
