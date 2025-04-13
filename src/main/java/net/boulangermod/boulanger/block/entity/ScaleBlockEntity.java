package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.screen.ScaleBlockMenu;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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

        // Basic pre-checks
        if (bowl.isEmpty() || bulk.isEmpty() || !bowl.is(Items.BOWL) || weightToTransfer <= 0) {
            System.out.println("[DEBUG] Pre-check failed: bowl empty, bulk empty, bowl not a bowl, or no weight to transfer.");
            return;
        }

        // 1) Determine the "per-unit" weight for the bulk item.
        // For flour items, useItemOn FlourType.
        // Otherwise, try WeightComponent; if not available, check FoodAdditiveComponent.
        FlourType flourType = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());
        float fullWeight;
        if (flourType != null) {
            fullWeight = flourType.getWeight();
            System.out.println("[DEBUG] Bulk recognized as flour; full weight from FlourType: " + fullWeight);
        } else if (bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())) {
            WeightComponent wc = bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
            fullWeight = wc.getWeight();
            System.out.println("[DEBUG] Bulk non-flour; full weight from WeightComponent: " + fullWeight);
        } else if (bulk.has(ModDataComponentTypes.FOOD_ADDITIVE.get())) {
            fullWeight = bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get()).getWeight();
            System.out.println("[DEBUG] Bulk non-flour; full weight from FoodAdditiveComponent: " + fullWeight);
        } else {
            fullWeight = 113f;
            System.out.println("[DEBUG] No component found on bulk; default full weight: " + fullWeight);
        }

        int stackCount = bulk.getCount();
        float totalAvailable = stackCount * fullWeight;
        System.out.println("[DEBUG] Total available weight = " + stackCount + " * " + fullWeight + " = " + totalAvailable);

        // 2) Determine weight to transfer.
        int transferAmount = Math.min((int) totalAvailable, weightToTransfer);
        System.out.println("[DEBUG] Transfer amount: " + transferAmount + " (target: " + weightToTransfer + ")");

        // 3) Create the filled bowl with transferred weight.
        ItemStack taggedBowl = new ItemStack(Items.BOWL);
        taggedBowl.set(ModDataComponentTypes.INGREDIENT_GRAMS, new WeightComponent(transferAmount));
        taggedBowl.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), IngredientCategory.getIngredientCategory(bulk));
        taggedBowl.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(bulk.getItem()));
        if (flourType != null) {
            taggedBowl.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
        }
        items.setStackInSlot(SLOT_BOWL_OUT, taggedBowl);
        System.out.println("[DEBUG] Created filled bowl with weight " + transferAmount + "g.");

        // 4) Compute remaining weight.
        float remainingTotal = totalAvailable - transferAmount;
        System.out.println("[DEBUG] Remaining weight after transfer: " + remainingTotal);

        // 5) Update the bulk and residual slots.
        boolean isFlour = (flourType != null);
        if (remainingTotal < 0.1f) {
            items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
            items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
            System.out.println("[DEBUG] Remaining weight negligible; cleared bulk and residual slots.");
        } else {
            int fullRemain = (int) (remainingTotal / fullWeight);
            float partialRemain = remainingTotal - fullRemain * fullWeight;
            System.out.println("[DEBUG] Full remaining items: " + fullRemain + " and partial remaining weight: " + partialRemain);

            if (isFlour) {
                // For flour: bulk gets full items, residual gets partial (if any).
                ItemStack newBulk = new ItemStack(bulk.getItem(), fullRemain);
                if (flourType != null) {
                    newBulk.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
                }
                items.setStackInSlot(SLOT_BULK, newBulk);
                System.out.println("[DEBUG] Flour: Updated bulk slot with " + fullRemain + " full items.");

                if (partialRemain > 0) {
                    ItemStack residual = new ItemStack(bulk.getItem(), 1);
                    residual.set(ModDataComponentTypes.INGREDIENT_GRAMS, new WeightComponent(partialRemain));
                    if (flourType != null) {
                        residual.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
                    }
                    items.setStackInSlot(SLOT_RESIDUAL, residual);
                    System.out.println("[DEBUG] Flour: Set residual slot with 1 partial item weighing " + partialRemain + "g.");
                } else {
                    items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
                    System.out.println("[DEBUG] Flour: No partial remain; cleared residual slot.");
                }
            } else {
                // For non-flour items: bulk slot holds only full items...
                if (fullRemain > 0) {
                    ItemStack newBulk = new ItemStack(bulk.getItem(), fullRemain);
                    newBulk.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(fullWeight));
                    items.setStackInSlot(SLOT_BULK, newBulk);
                    System.out.println("[DEBUG] Non-flour: Updated bulk slot with " + fullRemain + " full items.");
                } else {
                    // When there are no full units, clear the bulk slot.
                    items.setStackInSlot(SLOT_BULK, ItemStack.EMPTY);
                    System.out.println("[DEBUG] Non-flour: No full units remaining; cleared bulk slot.");
                }
                // And place the partial remainder into the residual slot.
                if (partialRemain > 0) {
                    ItemStack residual = new ItemStack(bulk.getItem(), 1);
                    residual.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(partialRemain));
                    items.setStackInSlot(SLOT_RESIDUAL, residual);
                    System.out.println("[DEBUG] Non-flour: Moved leftover to residual slot: 1 partial item weighing " + partialRemain + "g.");
                } else {
                    items.setStackInSlot(SLOT_RESIDUAL, ItemStack.EMPTY);
                    System.out.println("[DEBUG] Non-flour: No partial remain; cleared residual slot.");
                }
            }
        }

        // 6) Clear bowl input and reset transfer target.
        items.setStackInSlot(SLOT_BOWL_IN, ItemStack.EMPTY);
        weightToTransfer = 0;

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        System.out.println("[DEBUG] Transfer complete; updated block state.");
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // save the 4‑slot ItemStackHandler
        tag.put("Inventory", items.serializeNBT(registries));

        // save the current target‑weight setting
        tag.putInt("WeightToTransfer", this.weightToTransfer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // restore the ItemStackHandler
        items.deserializeNBT(registries, tag.getCompound("Inventory"));

        // restore the pending transfer weight
        this.weightToTransfer = tag.getInt("WeightToTransfer");
    }
}
