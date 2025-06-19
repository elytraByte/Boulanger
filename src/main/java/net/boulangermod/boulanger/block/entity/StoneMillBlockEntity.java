package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.StoneMillBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class StoneMillBlockEntity extends AbstractProcessingBlockEntity implements AbstractProcessingBlock.Tickable {
    private int millProgress = 0;
    private boolean milling = false;
    private static final int MAX_MILL_TIME = 200;

    public StoneMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_MILL_BE.get(), pos, state, 2);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.STONE_MILL_BE.get();
    }


    public void startMilling() {
        ItemStack inputStack = itemHandler.getStackInSlot(0);
        if (!inputStack.isEmpty()) {
            Item inputItem = inputStack.getItem();
            // Example: If input is wheat, produce your mod’s flour item.
            if (inputItem == Items.WHEAT) {
                // Create new output stack – here we assume ModItems.FLOUR_ITEM is your custom flour.
                ItemStack outputStack = new ItemStack(ModItems.FLOUR_ITEM.get(), 1);
                // Convert the whole wheat flour enum value into a FlourType instance.
                FlourType wholeWheatType = FlourItemType.WHOLE_WHEAT_FLOUR.toFlourType();
                // Attach the FlourType to the output stack using your data component system.
                // This ensures that the modelIndex (e.g. 17 for whole wheat flour) is stored.
                outputStack.set(ModDataComponentTypes.FLOUR_TYPE.get(), wholeWheatType);
                // Place the output stack into the output slot.
                itemHandler.setStackInSlot(1, outputStack);
                // Consume one unit from the input stack.
                inputStack.shrink(1);
                setChanged();
            }
        }
    }

    /**
     * Resets the milling progress and state.
     */
    public void resetMilling() {
        millProgress = 0;
        milling = false;
        setChanged();
    }

    /**
     * Helper method called by the menu to show progress.
     * This method is used interchangeably with getMixProgress() in your menu logic.
     */
    public int getMixProgress() {
        return millProgress;
    }

    /**
     * Helper method called by the menu to determine if the mill is active.
     */
    public boolean isMilling() {
        return milling;
    }

    /**
     * Optional method for useItemOn in the menu to get the total cycle time.
     */
    public static int getMaxMixTime() {
        return MAX_MILL_TIME;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("MillProgress", millProgress);
        tag.putBoolean("Milling", milling);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super .loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        millProgress = tag.getInt("MillProgress");
        milling = tag.getBoolean("Milling");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("stone_mill.boulanger");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new StoneMillBlockMenu(i, inventory, this);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        // Check if there is valid input and we are not already milling
        if (!milling && !itemHandler.getStackInSlot(0).isEmpty()) {
            startMilling();

            setChanged();
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }

        // When milling has started, progress the process.
        if (milling) {
            millProgress++;
            // When milling is complete, process the crafting step.
            if (millProgress >= MAX_MILL_TIME) {
                startMilling();
                resetMilling();
            }
            setChanged();
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
