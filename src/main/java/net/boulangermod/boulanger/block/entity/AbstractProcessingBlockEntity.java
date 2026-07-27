package net.boulangermod.boulanger.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for processing machines that own an inventory.
 * - Saves/loads inventory via HolderLookup (1.21).
 * - Handles S2C sync via BE data packet/tag.
 * - Provides item drops helper.
 * - Acts as a MenuProvider so children only implement createMenu (and optional getDisplayName).
 *
 * Capabilities: register handlers in RegisterCapabilitiesEvent, e.g.:
 *   event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TYPE, (be, side) -> be.getItemHandler(side));
 */
public abstract class AbstractProcessingBlockEntity extends BlockEntity implements MenuProvider {

    protected final ItemStackHandler itemHandler;
    protected final int slotCount;

    private int inventoryUpdateDepth = 0;
    private boolean inventoryChangedDuringUpdate = false;

    protected AbstractProcessingBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int slotCount
    ) {
        super(type, pos, state);

        this.slotCount = slotCount;

        this.itemHandler = new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                if (inventoryUpdateDepth > 0) {
                    inventoryChangedDuringUpdate = true;
                } else {
                    setChangedAndNotify();
                }
            }

            @Override
            public boolean isItemValid(
                    int slot,
                    ItemStack stack
            ) {
                return AbstractProcessingBlockEntity.this
                        .isItemValid(slot, stack);
            }
        };
    }

    protected boolean isItemValid(
            int slot,
            ItemStack stack
    ) {
        return true;
    }

    protected final void updateInventoryAtomically(
            Runnable mutation
    ) {
        inventoryUpdateDepth++;

        try {
            mutation.run();
        } finally {
            inventoryUpdateDepth--;

            if (inventoryUpdateDepth == 0
                    && inventoryChangedDuringUpdate) {
                inventoryChangedDuringUpdate = false;
                setChangedAndNotify();
            }
        }
    }

    protected void setChangedAndNotify() {
        setChanged();

        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    /** Expose the item handler to capability providers (side-gate here if needed). */
    public @Nullable ItemStackHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    /* ------------------------- Persistence ------------------------- */

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    /* --------------------------- Sync ----------------------------- */

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        loadAdditional(pkt.getTag(), registries);
    }


    /* --------------------------- Drops ---------------------------- */

    /** Called by AbstractProcessingBlock#onRemove via Tickable.drops(). */
    public void drops() {
        if (level == null || level.isClientSide) return;
        SimpleContainer container = new SimpleContainer(slotCount);
        for (int i = 0; i < slotCount; i++) {
            container.setItem(i, itemHandler.getStackInSlot(i).copy());
        }
        Containers.dropContents(level, worldPosition, container);
    }

    /* ----------------------- MenuProvider ------------------------- */

    /** Default display name: the block's translatable name. Children may override. */
    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    /** Force children that have UIs to supply their menu. */
    @Override
    public abstract AbstractContainerMenu createMenu(int id, Inventory inv, Player player);

    /* ----------------------- Convenience -------------------------- */

    public int getSlotCount() { return slotCount; }

    /** Force capability cache refresh if capability presence changes. */
    protected void invalidateBlockCaps() {
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }
}