package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.WoodOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WoodOvenBlockEntity extends BlockEntity implements AbstractProcessingBlock.Tickable, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public WoodOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_OVEN_BE.get(), pos, state);
    }

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int cookTime = 0;
    private static final int MAX_COOK_TIME = 200; // Standard furnace time

    public int getBurnTime() { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public int getCookTime() { return cookTime; }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean wasBurning = isBurning();

        if (burnTime > 0) {
            burnTime--;
        }

        boolean canSmelt = canCook();
        ItemStack fuelStack = itemHandler.getStackInSlot(SLOT_FUEL);

        if (burnTime == 0 && canSmelt && fuelStack.getItem() == ModItems.SPLIT_PINE_LOGS.get()) {
            burnTime = (int)(160 * 1.5f);
            maxBurnTime = burnTime;
            fuelStack.shrink(1);
        }

        if (isBurning() && canSmelt) {
            cookTime++;
            if (cookTime >= MAX_COOK_TIME) {
                cookTime = 0;
                cook();
            }
        } else {
            cookTime = 0;
        }

        if (wasBurning != isBurning()) {
            setChanged();
        }


    }

    private boolean canCook() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        if (input.isEmpty() || input.getItem() != ModItems.DOUGH.get()) return false;
        if (output.isEmpty()) return true;
        if (output.getItem() != Items.BREAD) return false;
        return output.getCount() < output.getMaxStackSize();
    }

    private void cook() {
        if (!canCook()) return;

        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        if (output.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, new ItemStack(Items.BREAD));
        } else {
            output.grow(1);
        }

        input.shrink(1);


    }

    private boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    public void drops() {
        SimpleContainer container = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            container.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("woodoven.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WoodOvenMenu(id, inventory, this);
    }

//    @Override
//    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
//        super.saveAdditional(pTag, pRegistries);
//        ContainerHelper.saveAllItems(pTag, inventory, pRegistries);
//    }
//
//    @Override
//    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
//        super.loadAdditional(pTag, pRegistries);
//        ContainerHelper.loadAllItems(pTag, inventory, pRegistries);
//    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }


}
