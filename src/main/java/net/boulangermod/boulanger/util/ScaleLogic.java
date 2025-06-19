package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.item.FiftyPoundBagItem;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ScaleLogic {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int WATER_BUCKET_GRAMS = 4000;

    private ScaleLogic() {}

    /**
     * Pull up to requestedMg milligrams out of the bulk stack,
     * and return both how much actually moved and the new
     * bulk + residual stacks.
     */
    public static TransferResult transfer(ItemStack bulk, int requestedMg) {
        // 0) sanity
        if (bulk.isEmpty() || requestedMg <= 0) {
            return new TransferResult(0, ItemStack.EMPTY, ItemStack.EMPTY);
        }

        // 1) special‐case water bucket
        if (bulk.getItem() == Items.WATER_BUCKET) {
            int wantMg = Math.min(requestedMg, WATER_BUCKET_GRAMS * 1000);
            // bucket yields 4000 g = 4 000 000 mg
            ItemStack residual = new ItemStack(Items.BUCKET);
            return new TransferResult(wantMg, ItemStack.EMPTY, residual);
        }

        // 2) figure grams per “unit”
        float perUnit;
        if (bulk.get(ModDataComponentTypes.FLOUR_TYPE.get()) != null) {
            perUnit = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get()).getWeight();
        } else if (bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())) {
            perUnit = bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight();
        } else if (bulk.has(ModDataComponentTypes.FOOD_ADDITIVE.get())) {
            perUnit = bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get()).getWeight();
        } else if (bulk.getItem() == Items.SUGAR) {
            perUnit = 113f;
        } else {
            perUnit = 0f;
        }

        boolean isBag = bulk.getItem() instanceof FiftyPoundBagItem &&
                bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        // 3) total available in mg
        float totalGrams = isBag
                ? bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight()
                : bulk.getCount() * perUnit;
        long totalMg   = Math.round(totalGrams * 1000f);

        // 4) what we actually move
        int transferMg = (int)Math.min(totalMg, (long)requestedMg);
        long remainMg  = totalMg - transferMg;
        float remainG  = remainMg / 1000f;

        // 5) build new bulk + residual
        ItemStack newBulk   = ItemStack.EMPTY;
        ItemStack residual  = ItemStack.EMPTY;

        if (isBag) {
            if (remainMg > 0) {
                newBulk = bulk.copy();
                newBulk.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                        new WeightComponent(remainG));
            }
        } else {
            int fullCount = perUnit > 0 ? (int)(remainG / perUnit) : 0;
            float leftover = remainG - fullCount * perUnit;
            if (fullCount > 0) {
                newBulk = bulk.copy();
                newBulk.setCount(fullCount);
            }
            if (leftover > 0f) {
                residual = bulk.copy();
                residual.setCount(1);
                residual.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                        new WeightComponent(leftover));
            }
        }

        LOGGER.debug("transfer: requested={} mg, moved={} mg, remain={} mg",
                requestedMg, transferMg, remainMg);
        return new TransferResult(transferMg, newBulk, residual);
    }

    /**
     * Given the original bulk and how many mg we moved,
     * produce a properly‐tagged FilledBowl item.
     */
    public static ItemStack createFilledBowl(ItemStack bulk, int movedMg) {
        ItemStack bowl = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());
        // WeightComponent takes grams
        float movedG = movedMg / 1000f;
        bowl.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                new WeightComponent(movedG));
        bowl.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(),
                IngredientCategory.getIngredientCategory(bulk));

        Item ing = bulk.getItem() instanceof FiftyPoundBagItem
                ? ModItems.FLOUR_ITEM.get()
                : bulk.getItem();
        bowl.set(ModDataComponentTypes.INGREDIENT_TYPE.get(),
                new IngredientTypeComponent(ing));

        FlourType ft = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (ft != null) {
            bowl.set(ModDataComponentTypes.FLOUR_TYPE.get(), ft);
        }
        return bowl;
    }

    public static class TransferResult {
        public final int transferredMg;
        public final ItemStack newBulkStack;
        public final ItemStack residualStack;
        public TransferResult(int mg, ItemStack bulk, ItemStack res) {
            this.transferredMg   = mg;
            this.newBulkStack    = bulk;
            this.residualStack   = res;
        }
    }
}