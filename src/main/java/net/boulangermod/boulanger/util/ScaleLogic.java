package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.*;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ScaleLogic {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int   WATER_BUCKET_GRAMS     = 4000;
    private static final float SUGAR_GRAMS_PER        = 113f;  // 1 sugar
    private static final float HONEY_BOTTLE_GRAMS_PER = 454f;  // 1 honey bottle

    private ScaleLogic() {}

    public static TransferResult transfer(ItemStack bulk, int requestedMg) {
        if (bulk.isEmpty() || requestedMg <= 0) {
            return new TransferResult(0, ItemStack.EMPTY, ItemStack.EMPTY);
        }

        if (bulk.getItem() == Items.WATER_BUCKET) {
            int wantMg = Math.min(requestedMg, WATER_BUCKET_GRAMS * 1000);
            ItemStack residual = new ItemStack(Items.BUCKET);
            return new TransferResult(wantMg, ItemStack.EMPTY, residual);
        }

        // grams per *unit* (or explicit grams if the stack carries a weight component)
        float perUnit;
        if (bulk.get(ModDataComponentTypes.FLOUR_TYPE.get()) != null) {
            perUnit = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get()).getWeight();
        } else if (bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())) {
            perUnit = bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight();
        } else if (bulk.has(ModDataComponentTypes.FOOD_ADDITIVE.get())) {
            perUnit = bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get()).getWeight();
        } else if (bulk.getItem() instanceof FoodAdditiveItem fai && fai.getType() != null) {
            perUnit = fai.getType().getWeight();
        } else if (bulk.getItem() instanceof BakeryAdditiveItem bai && bai.getType() != null) {
            perUnit = bai.getType().getWeight();
        } else if (bulk.getItem() == Items.SUGAR) {
            perUnit = SUGAR_GRAMS_PER;
        } else if (bulk.getItem() == Items.HONEY_BOTTLE) {
            perUnit = HONEY_BOTTLE_GRAMS_PER;
        } else {
            perUnit = 0f;
        }

        boolean isBag = bulk.getItem() instanceof FiftyPoundBagItem
                && bulk.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        float totalGrams = isBag
                ? bulk.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).getWeight()
                : bulk.getCount() * perUnit;
        long totalMg = Math.round(totalGrams * 1000f);

        int  transferMg = (int) Math.min(totalMg, (long) requestedMg);
        long remainMg   = totalMg - transferMg;
        float remainG   = remainMg / 1000f;

        ItemStack newBulk  = ItemStack.EMPTY;
        ItemStack residual = ItemStack.EMPTY;

        if (isBag) {
            if (remainMg > 0) {
                newBulk = bulk.copy();
                newBulk.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(remainG));
            }
        } else {
            int   fullCount = perUnit > 0 ? (int) (remainG / perUnit) : 0;
            float leftover  = remainG - fullCount * perUnit;

            if (fullCount > 0) {
                newBulk = bulk.copy();
                newBulk.setCount(fullCount);
            }
            if (leftover > 0f) {
                residual = bulk.copy();
                residual.setCount(1);
                residual.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(leftover));
                copyIdentityComponents(bulk, residual);
            }
        }

        LOGGER.debug("transfer: requested={} mg, moved={} mg, remain={} mg",
                requestedMg, transferMg, remainMg);
        return new TransferResult(transferMg, newBulk, residual);
    }

    /** Build a FILLED_BOWL_ITEM fully stamped with weight, category, source item, flour/additive identity. */
    public static ItemStack createFilledBowl(ItemStack bulk, int movedMg) {
        ItemStack bowl = new ItemStack(ModItems.FILLED_BOWL_ITEM.get());

        float movedG = movedMg / 1000f;
        bowl.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(movedG));

        Item sourceItem = resolveSourceItem(bulk);
        bowl.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(sourceItem));

        // Flour identity (if any)
        FlourType ft = bulk.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (ft != null) {
            bowl.set(ModDataComponentTypes.FLOUR_TYPE.get(), ft);
        }

        // Additive identity: both FoodAdditiveItem and BakeryAdditiveItem write FOOD_ADDITIVE
        FoodAdditiveComponent foodAdd = bulk.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
        if (foodAdd == null) {
            if (bulk.getItem() instanceof FoodAdditiveItem fai && fai.getType() != null) {
                foodAdd = fai.getType().toFoodAdditiveComponent();
            } else if (bulk.getItem() instanceof BakeryAdditiveItem bai && bai.getType() != null) {
                // bakery additives also serialized into FoodAdditiveComponent
                foodAdd = bai.getType().toFoodAdditiveComponent();
            }
        }
        if (foodAdd != null) {
            bowl.set(ModDataComponentTypes.FOOD_ADDITIVE.get(), foodAdd);
        }

        // Category — explicit > enum-derived > vanilla fallback > heuristic
        IngredientCategory cat = deriveCategoryFromBulk(bulk, ft, foodAdd, sourceItem);
        bowl.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), cat);

        return bowl;
    }

    // ---------- helpers ----------

    private static IngredientCategory deriveCategoryFromBulk(
            ItemStack bulk,
            FlourType ft,
            FoodAdditiveComponent foodAdd,
            Item sourceItem
    ) {
        IngredientCategory explicit = bulk.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        if (explicit != null) return explicit;

        if (ft != null || bulk.getItem() instanceof FiftyPoundBagItem || sourceItem == ModItems.FLOUR_ITEM.get()) {
            return IngredientCategory.FLOUR;
        }
        if (bulk.getItem() instanceof FoodAdditiveItem fai && fai.getType() != null) {
            return fai.getType().getCategory();
        }
        if (bulk.getItem() instanceof BakeryAdditiveItem bai && bai.getType() != null) {
            return bai.getType().getCategory();
        }
        if (foodAdd != null) {
            // If you later add a category into FoodAdditiveComponent, map it here.
            return IngredientCategory.ADDITIVE;
        }
        if (bulk.getItem() == Items.SUGAR)        return IngredientCategory.SUGAR;
        if (bulk.getItem() == Items.HONEY_BOTTLE) return IngredientCategory.SUGAR;
        if (bulk.getItem() == Items.MILK_BUCKET)  return IngredientCategory.DAIRY;
        if (bulk.getItem() == Items.WATER_BUCKET) return IngredientCategory.WATER;

        return IngredientCategory.getIngredientCategory(bulk);
    }

    private static Item resolveSourceItem(ItemStack bulk) {
        IngredientTypeComponent itc = bulk.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (itc != null && itc.item() != null) return itc.item();
        if (bulk.getItem() instanceof FiftyPoundBagItem) return ModItems.FLOUR_ITEM.get();
        return bulk.getItem();
    }

    private static void copyIdentityComponents(ItemStack from, ItemStack to) {
        var ft = from.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (ft != null) to.set(ModDataComponentTypes.FLOUR_TYPE.get(), ft);

        var fa = from.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
        if (fa != null) to.set(ModDataComponentTypes.FOOD_ADDITIVE.get(), fa);

        var itc = from.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (itc != null) to.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), itc);

        var cat = from.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        if (cat != null) to.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), cat);
    }

    public static class TransferResult {
        public final int transferredMg;
        public final ItemStack newBulkStack;
        public final ItemStack residualStack;
        public TransferResult(int mg, ItemStack bulk, ItemStack res) {
            this.transferredMg = mg;
            this.newBulkStack  = bulk;
            this.residualStack = res;
        }
    }
}
