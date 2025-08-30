package net.boulangermod.boulanger.util;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Canonicalize DoughRecipe for deterministic serialization / equality:
 *  - sort ingredients by (category priority, then itemId)
 *  - sort targetPercentages (keys & values stay aligned) by category priority
 *  - round percentages to fixed precision
 *
 *  Call this BEFORE writing your recipe into the DOUGH_RECIPE data component.
 */
public final class DoughRecipeCanonicalier {

    private DoughRecipeCanonicalier() {}

    /** Adjust if you want a different stable order. Unknowns fall to the end. */
    private static final List<String> CATEGORY_ORDER = List.of(
            "FLOUR", "WATER", "DAIRY", "EGGS", "FAT", "SUGAR", "SALT", "YEAST", "ADDITIVE"
    );
    private static final Map<String, Integer> CAT_PRI = IntStream.range(0, CATEGORY_ORDER.size())
            .boxed()
            .collect(Collectors.toUnmodifiableMap(CATEGORY_ORDER::get, i -> i));

    /** Round % values to N decimals to avoid float wobble in NBT/JSON. */
    private static final int PCT_DECIMALS = 3;
    private static final double PCT_SCALE = Math.pow(10, PCT_DECIMALS);

    private static int categoryOrder(String cat) {
        return CAT_PRI.getOrDefault(cat, Integer.MAX_VALUE / 2);
    }

    private static double roundPct(double v) {
        return Math.round(v * PCT_SCALE) / PCT_SCALE;
    }

    /** Your ingredient entry – adjust to your actual type if different. */
    public record Ingredient(String category, ResourceLocation itemId, int milligrams) {}

    /** Your dough recipe – adjust to your actual type if different. */
    public record DoughRecipe(
            ResourceLocation recipeId,
            int totalWeight,                                // grams
            List<String> targetPercentagesKeys,             // e.g., ["FLOUR","WATER","SALT","YEAST"]
            List<Double> targetPercentagesValues,           // aligned  [100.0,   70.0,   3.0,   4.0]
            List<Ingredient> ingredients
    ){}

    /** Return a new, canonicalized recipe without mutating the input. */
    public static DoughRecipe canonicalize(DoughRecipe in) {
        if (in == null) return null;

        // 1) Coalesce duplicate ingredients (same category + itemId) and keep mg as ints.
        Map<String, Map<ResourceLocation, Integer>> merged = new HashMap<>();
        for (Ingredient ing : in.ingredients()) {
            merged.computeIfAbsent(ing.category(), k -> new HashMap<>())
                    .merge(ing.itemId(), Math.max(0, ing.milligrams()), Integer::sum);
        }
        // Rebuild list, sorted by (category priority → itemId)
        List<Ingredient> ingredients = merged.entrySet().stream()
                .flatMap(e -> e.getValue().entrySet().stream()
                        .map(entry -> new Ingredient(e.getKey(), entry.getKey(), entry.getValue())))
                .sorted(Comparator
                        .comparingInt((Ingredient i) -> categoryOrder(i.category()))
                        .thenComparing(i -> i.itemId().toString()))
                .toList();

        // 2) Sort keys & keep values aligned; round values
        List<String> keys = new ArrayList<>(in.targetPercentagesKeys());
        List<Double> vals = new ArrayList<>(in.targetPercentagesValues());

        int n = Math.min(keys.size(), vals.size());
        List<Map.Entry<String, Double>> kv = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            kv.add(new AbstractMap.SimpleEntry<>(keys.get(i), roundPct(vals.get(i))));
        }
        kv.sort(Comparator
                .comparingInt((Map.Entry<String, Double> e) -> categoryOrder(e.getKey()))
                .thenComparing(Map.Entry::getKey));

        List<String> sortedKeys  = kv.stream().map(Map.Entry::getKey).toList();
        List<Double> sortedVals  = kv.stream().map(Map.Entry::getValue).toList();

        // 3) Return a clean, stable object
        return new DoughRecipe(
                in.recipeId(),
                in.totalWeight(),
                sortedKeys,
                sortedVals,
                ingredients
        );
    }

    /**
     * Helper you can call on an ItemStack after you’ve populated its recipe component.
     * Adjust the getter/setter to your actual component type.
     */
    public static void canonicalizeOnStack(net.minecraft.world.item.ItemStack stack,
                                           net.minecraft.core.component.DataComponentType<DoughRecipe> recipeType) {
        DoughRecipe recipe = stack.get(recipeType);
        if (recipe != null) {
            stack.set(recipeType, canonicalize(recipe));
        }
    }
}
