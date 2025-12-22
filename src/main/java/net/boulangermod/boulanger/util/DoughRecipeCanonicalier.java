package net.boulangermod.boulanger.util;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/**
 * Deterministic representation of a dough recipe.
 * Sorts categories by {@link IngredientCategory} order (fallback: lexicographic),
 * sorts ingredients by (category, itemId), and rounds baker % values.
 *
 * Use this whenever you write/read the recipe component so equality checks and
 * serialization are stable across runs.
 */
public final class DoughRecipeCanonicalier {
    private DoughRecipeCanonicalier() {}

    // round baker % values to 0.001%
    private static final double PCT_SCALE = 1000.0;

    /** A single weighed ingredient. */
    public record Ingredient(String category, ResourceLocation itemId, int milligrams) {}

    /** Canonical dough recipe payload. */
    public record DoughRecipe(
            ResourceLocation recipeId,
            int totalWeightGrams,
            List<String> targetPercentKeys,     // e.g. ["FLOUR","WATER","SALT","YEAST"]
            List<Double> targetPercentValues,   // same length as keys
            List<Ingredient> ingredients        // sorted (category then itemId)
    ) {}

    /** Build from pieces and canonicalize. */
    public static DoughRecipe canonicalizeFromPieces(ResourceLocation recipeId,
                                                     int totalWeightGrams,
                                                     Map<String, Double> targetPercentages,
                                                     List<Ingredient> ingredients) {
        List<String> keys = new ArrayList<>(targetPercentages.keySet());
        List<Double> vals = new ArrayList<>(keys.size());
        for (String k : keys) vals.add(targetPercentages.getOrDefault(k, 0.0));
        return canonicalize(new DoughRecipe(recipeId, totalWeightGrams, keys, vals, ingredients));
    }

    /** Canonicalize an existing recipe object. */
    public static DoughRecipe canonicalize(DoughRecipe in) {
        Comparator<String> catOrder = categoryComparator();

        // 1) sort % keys/values together
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < in.targetPercentKeys.size(); i++) idx.add(i);
        idx.sort((a, b) -> catOrder.compare(in.targetPercentKeys.get(a), in.targetPercentKeys.get(b)));

        List<String> keys = new ArrayList<>(in.targetPercentKeys.size());
        List<Double> vals = new ArrayList<>(in.targetPercentValues.size());
        for (int i : idx) {
            keys.add(in.targetPercentKeys.get(i));
            vals.add(roundPct(in.targetPercentValues.get(i)));
        }

        // 2) sort ingredients by (category, itemId, milligrams)
        List<Ingredient> ings = new ArrayList<>(in.ingredients);
        ings.sort((a, b) -> {
            int c = catOrder.compare(a.category(), b.category());
            if (c != 0) return c;
            int ns = a.itemId().getNamespace().compareTo(b.itemId().getNamespace());
            if (ns != 0) return ns;
            int path = a.itemId().getPath().compareTo(b.itemId().getPath());
            if (path != 0) return path;
            return Integer.compare(a.milligrams(), b.milligrams());
        });

        return new DoughRecipe(in.recipeId, in.totalWeightGrams, keys, vals, ings);
    }

    /** Rounds a percent value to 0.001% */
    private static double roundPct(double v) {
        return Math.round(v * PCT_SCALE) / PCT_SCALE;
    }

    /** Order categories by enum ordinal first, then lexicographically for unknowns. */
    private static Comparator<String> categoryComparator() {
        Map<String, Integer> pri = new HashMap<>();
        int i = 0;
        for (IngredientCategory c : IngredientCategory.values()) {
            pri.put(c.name(), i++);
        }
        return Comparator
                .comparingInt((String s) -> pri.getOrDefault(s, Integer.MAX_VALUE))
                .thenComparing(Comparator.naturalOrder());
    }

    /**
     * Helper to canonicalize a recipe already on an ItemStack.
     * Replace the get/set calls with your actual data component if needed.
     */
    public static void canonicalizeOnStack(
            net.minecraft.world.item.ItemStack stack,
            net.minecraft.core.component.DataComponentType<DoughRecipe> recipeType) {
        DoughRecipe r = stack.get(recipeType);
        if (r != null) stack.set(recipeType, canonicalize(r));
    }
}
