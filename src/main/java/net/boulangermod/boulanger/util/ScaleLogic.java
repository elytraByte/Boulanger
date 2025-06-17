package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.item.FiftyPoundBagItem;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ScaleLogic {
    private ScaleLogic() {}

    public static ItemStack createFilledBowl(ItemStack bulk, int toTransfer) {
        ItemStack filled = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());
        filled.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(toTransfer));
        filled.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(),
                IngredientCategory.getIngredientCategory(bulk));
        Item ingredientItem = (bulk.getItem() instanceof FiftyPoundBagItem)
                ? ModItems.FLOUR_ITEM.get()
                : bulk.getItem();
        filled.set(ModDataComponentTypes.INGREDIENT_TYPE.get(),
                new IngredientTypeComponent(ingredientItem));

        FlourType flourType = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (flourType != null) {
            filled.set(ModDataComponentTypes.FLOUR_TYPE.get(), flourType);
        }
        return filled;
    }

    public static TransferResult transfer(
            ItemStack bulk,
            int requestedGrams
    ) {
        // compute perUnit, totalAvailable exactly like in your block entity
        FlourType flourType = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());
        float perUnit = flourType != null
                ? flourType.getWeight()
                : bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())
                ? bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight()
                : bulk.has(ModDataComponentTypes.FOOD_ADDITIVE.get())
                ? bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get()).getWeight()
                : 113f;

        float totalAvailable;
        boolean isBulkBag = bulk.getItem() instanceof FiftyPoundBagItem &&
                bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        if (isBulkBag) {
            totalAvailable = bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).grams();
            perUnit = totalAvailable;
        } else {
            totalAvailable = bulk.getCount() * perUnit;
        }

        int toTransfer = Math.min((int) totalAvailable, requestedGrams);
        float remaining = totalAvailable - toTransfer;

        // figure out new bulk / residual as in your block
        ItemStack newBulk = ItemStack.EMPTY;
        ItemStack residual = ItemStack.EMPTY;

        if (isBulkBag) {
            if (remaining > 0) {
                newBulk = bulk.copy();
                newBulk.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                        new WeightComponent((int) remaining));
            }
        } else {
            int fullRemain = (int)(remaining / perUnit);
            float partial = remaining - fullRemain * perUnit;
            if (fullRemain > 0) {
                newBulk = bulk.copy();
                newBulk.setCount(fullRemain);
            }
            if (partial > 0f) {
                residual = bulk.copy();
                residual.setCount(1);
                residual.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                        new WeightComponent((int)partial));
            }
        }

        return new TransferResult(toTransfer, newBulk, residual);
    }

    public static class TransferResult {
        public final int transferred;
        public final ItemStack newBulkStack;
        public final ItemStack residualStack;
        public TransferResult(int t, ItemStack bulk, ItemStack res) {
            this.transferred = t;
            this.newBulkStack = bulk;
            this.residualStack = res;
        }
    }
}

