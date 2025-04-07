package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.WoodOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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

    private static final int MAX_BURN_TIME = 200;
    private int burnTime = 0;
    private boolean isBurning = false;

    public WoodOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_OVEN_BE.get(), pos, state);
    }

    public class DoughItem extends Item {
        private final Supplier<Item> bakedResult;

        public DoughItem(Properties properties, Supplier<Item> bakedResult) {
            super(properties);
            this.bakedResult = bakedResult;
        }

        public Item getBakedResult() {
            return bakedResult.get();
        }

        public boolean isDough() {
            return true;
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (hasFuelItem() && !isBurning) {
            startBurning();
        }

        if (isBurning) {
            burnTime--;
            if (canBake()) {
                bakeItem();
            }
            if (burnTime <= 0) {
                stopBurning();
            }
        }
    }

    private boolean canBake() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);

        if (!(input.getItem() instanceof DoughItem dough)) return false;
        Item result = dough.getBakedResult();
        if (result == null) return false;

        ItemStack resultStack = new ItemStack(result);

        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, resultStack) && output.getCount() + 1 <= output.getMaxStackSize());
    }

    private void bakeItem() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (!(input.getItem() instanceof DoughItem dough)) return;

        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack result = new ItemStack(dough.getBakedResult());

        input.shrink(1);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(1);
        }
    }

    private boolean hasFuelItem() {
        ItemStack fuelStack = itemHandler.getStackInSlot(SLOT_FUEL);
        return fuelStack.getItem() == ModItems.SPLIT_PINE_LOGS.get();
    }

    private void startBurning() {
        itemHandler.extractItem(SLOT_FUEL, 1, false);
        burnTime = MAX_BURN_TIME;
        isBurning = true;
    }

    private void stopBurning() {
        isBurning = false;
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
}
