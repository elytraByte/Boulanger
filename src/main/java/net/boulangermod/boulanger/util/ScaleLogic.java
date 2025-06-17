package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.item.FiftyPoundBagItem;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.world.item.Items;              // ← import vanilla sugar
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ScaleLogic {
    private static final Logger LOGGER = LogManager.getLogger();
    private ScaleLogic() {}

    /**
     * Create a bowl tagged with exactly transferMg milligrams of ingredient.
     */
    public static ItemStack createFilledBowl(ItemStack bulk, int transferMg) {
        float grams = (float) transferMg / 1000f;
        LOGGER.debug("createFilledBowl: transferMg={} mg  →  grams={} g", transferMg, grams);

        ItemStack filled = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());
        filled.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(grams));
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

    /**
     * Pull up to requestedMg milligrams out of the bulk stack.
     */
    public static TransferResult transfer(ItemStack bulk, int requestedMg) {
        // 1) pick grams per “unit”...
        float perUnitGrams;

        // → sugar override: 113 g each
        if (bulk.getItem() == Items.SUGAR) {
            perUnitGrams = 113f;
        }
        // → custom flour types
        else if (bulk.get(ModDataComponentTypes.FLOUR_TYPE.get()) != null) {
            perUnitGrams = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get()).getWeight();
        }
        // → any item tagged with INGREDIENT_GRAMS
        else if (bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())) {
            perUnitGrams = bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight();
        }
        // → any food additive tagged
        else if (bulk.has(ModDataComponentTypes.FOOD_ADDITIVE.get())) {
            perUnitGrams = bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get()).getWeight();
        }
        // → nothing else has weight
        else {
            perUnitGrams = 0f;
        }

        boolean isBulkBag = bulk.getItem() instanceof FiftyPoundBagItem
                && bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        // 2) compute total available
        float totalAvailableGrams = isBulkBag
                ? bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight()
                : bulk.getCount() * perUnitGrams;

        // 3) to milligrams
        long totalAvailableMg = Math.round(totalAvailableGrams * 1000f);
        int transferMg      = (int)Math.min(totalAvailableMg, (long)requestedMg);
        long remainingMg    = totalAvailableMg - transferMg;
        float remainingGrams = remainingMg / 1000f;

        LOGGER.debug("transfer: requestedMg={} mg, totalAvailableMg={} mg, willTransfer={} mg, remain={} mg",
                requestedMg, totalAvailableMg, transferMg, remainingMg);

        // 4) split into new bulk + any leftover partial
        ItemStack newBulk  = ItemStack.EMPTY;
        ItemStack residual = ItemStack.EMPTY;

        if (isBulkBag) {
            if (remainingMg > 0) {
                newBulk = bulk.copy();
                newBulk.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                        new WeightComponent(remainingGrams));
            }
        } else {
            int fullRemain     = (int)(remainingGrams / perUnitGrams);
            float leftoverGrams = remainingGrams - fullRemain * perUnitGrams;

            if (fullRemain > 0) {
                newBulk = bulk.copy();
                newBulk.setCount(fullRemain);
            }
            if (leftoverGrams > 0f) {
                residual = bulk.copy();
                residual.setCount(1);
                residual.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                        new WeightComponent(leftoverGrams));
            }
        }

        return new TransferResult(transferMg, newBulk, residual);
    }

    public static class TransferResult {
        /** how many milligrams actually moved */
        public final int transferredMg;
        /** updated bulk after removal */
        public final ItemStack newBulkStack;
        /** any leftover “partial” unit */
        public final ItemStack residualStack;

        public TransferResult(int transferMg, ItemStack newBulk, ItemStack residual) {
            this.transferredMg = transferMg;
            this.newBulkStack  = newBulk;
            this.residualStack = residual;
        }
    }
}
