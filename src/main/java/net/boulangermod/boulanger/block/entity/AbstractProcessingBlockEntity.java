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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractProcessingBlockEntity extends BlockEntity implements MenuProvider {
    protected final ItemStackHandler itemHandler;

    public AbstractProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);


        this.itemHandler = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (!level.isClientSide()) {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }
        };
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> createTickerHelper(
            BlockEntityType<T> actualType,
            BlockEntityType<T> expectedType,
            BlockEntityTicker<? super T> ticker
    ) {
        return actualType == expectedType
                ? (BlockEntityTicker<T>) ticker
                : null;
    }


    public abstract BlockEntityType<?> getType();

    public abstract AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player);

    protected static void drops(Level level, BlockPos pos, ItemStackHandler handler) {
        SimpleContainer inventory = new SimpleContainer(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            inventory.setItem(i, handler.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Processing Block");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("inventory", itemHandler.serializeNBT(provider));
        super.saveAdditional(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        itemHandler.deserializeNBT(provider, tag.getCompound("inventory"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        super.onDataPacket(net, pkt, provider);
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public @Nullable IFluidHandler getFluidHandler(Direction side) {
        return this.getFluidHandler(side);
    }
}
