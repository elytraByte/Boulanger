package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.ScaleBlockMenu;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ScaleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BULK_INGREDIENT_SLOT = 0;
    public static final int BOWL_INPUT = 1;
    public static final int BOWL_OUTPUT = 2;
    public static final int REMAINDER_OUTPUT = 3;


    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    private int weightToTransfer = 0;

    public ScaleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCALE_BLOCK_BE.get(), pos, state);
    }

    public void setTargetWeight(int grams) {
        this.weightToTransfer = grams;
    }

    public static final int SLOT_BULK = 0;
    public static final int SLOT_BOWL_IN = 1;
    public static final int SLOT_BOWL_OUT = 2;
    public static final int SLOT_RESIDUAL = 3;

    public void transferToBowl() {
        if (level == null || level.isClientSide()) return;

        ItemStack bowl = items.getStackInSlot(SLOT_BOWL_IN);
        ItemStack bulk = items.getStackInSlot(SLOT_BULK);

        if (bowl.isEmpty() || bulk.isEmpty() || !bowl.is(Items.BOWL) || weightToTransfer <= 0) return;

        // 1) Get per-unit full weight from the flour type component
        FlourType flourType = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());
        float fullWeight = flourType != null ? flourType.getWeight() : 113f;

        int stackCount = bulk.getCount();
        float totalAvailable = stackCount * fullWeight;

        // 2) Determine how much to transfer
        int transferAmount = Math.min((int) totalAvailable, weightToTransfer);

        // 3) Create the filled bowl
        ItemStack taggedBowl = new ItemStack(Items.BOWL);
        taggedBowl.set(ModDataComponentTypes.INGREDIENT_GRAMS, new WeightComponent(transferAmount));
        taggedBowl.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.getIngredientCategory(bulk));
        taggedBowl.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(bulk.getItem()));
        if (flourType != null) {
            taggedBowl.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
        }
        items.setStackInSlot(SLOT_BOWL_OUT, taggedBowl);

        // 4) Compute remaining total
        float remainingTotal = totalAvailable - transferAmount;

        if (remainingTotal <= 0) {
            // No flour left at all
            items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
            items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
        } else {
            // 5a) How many full items remain?
            int fullRemain = (int) (remainingTotal / fullWeight);
            // 5b) Partial leftover for one item
            float partialRemain = remainingTotal - fullRemain * fullWeight;

            // Slot 0: fullRemain items
            ItemStack newBulk = new ItemStack(bulk.getItem(), fullRemain);
            // Copy other components if needed (e.g. FlourType)
            if (flourType != null) newBulk.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
            // No weight component on these full units (they implicitly weigh fullWeight each)
            items.setStackInSlot(SLOT_BULK, newBulk);

            // Slot 3: one partial item
            if (partialRemain > 0) {
                ItemStack residual = new ItemStack(bulk.getItem(), 1);
                residual.set(ModDataComponentTypes.INGREDIENT_GRAMS, new WeightComponent(partialRemain));
                if (flourType != null) residual.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
                items.setStackInSlot(SLOT_RESIDUAL, residual);
            } else {
                // No partial remain
                items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
            }
        }

        // 6) Clear the input bowl slot and reset
        items.setStackInSlot(SLOT_BOWL_IN, ItemStack.EMPTY);
        weightToTransfer = 0;

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }







    public ItemStackHandler getItems() {
        return items;
    }

    public void setGramsToWeigh(int grams) {
        this.weightToTransfer = grams;
    }

    public ItemStackHandler getItemHandler() {
        return items;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("scale.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ScaleBlockMenu(id, inventory, this);
    }
}
