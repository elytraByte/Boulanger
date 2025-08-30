package net.boulangermod.boulanger.trade;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class BreadTradeMatcher {
    private static final String MODID = "boulanger";
    private static final ResourceLocation GENERIC_FLOUR_ID = ResourceLocation.fromNamespaceAndPath(MODID, "flour");

    public record Tolerance(int perIngredientMg, int totalMg) {}

    private static ResourceLocation normalizeFlourId(String itemId, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FlourType> ft) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return ResourceLocation.fromNamespaceAndPath("minecraft", "air");
        if (id.equals(GENERIC_FLOUR_ID) && ft.isPresent() && ft.get().type() != null && !ft.get().type().isBlank()) {
            ResourceLocation typed = ResourceLocation.fromNamespaceAndPath(MODID, ft.get().type()); // e.g. bread_flour, vital_wheat_gluten
            return typed;
        }
        return id;
    }

    private static String key(IngredientInfo info) {
        String cat = info.category().name();
        ResourceLocation id = info.category() == IngredientCategory.FLOUR
                ? normalizeFlourId(info.itemId(), Optional.ofNullable(info.flourType()))
                : ResourceLocation.tryParse(info.itemId());
        return cat + "|" + (id == null ? "minecraft:air" : id.toString());
    }

    /** Build a mg map keyed by (category|itemIdNormalized). */
    private static Map<String, Integer> toMap(DoughRecipeComponent comp) {
        Map<String, Integer> map = new HashMap<>();
        for (IngredientInfo info : comp.ingredients()) {
            map.merge(key(info), Math.max(0, info.milligrams()), Integer::sum);
        }
        return map;
    }

    public static boolean matchesWithinTolerance(ItemStack playerStack,
                                                 DoughRecipeComponent expected,
                                                 Tolerance tol,
                                                 @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<PanTypeComponent> expectedPanOpt,
                                                 @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ResourceLocation> expectedRecipeIdOpt) {
        // Must be the right item
        if (!playerStack.is(ModItems.BREAD.get())) return false;

        // Required components present
        DoughRecipeComponent actual = playerStack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (actual == null) return false;

        // Optional pan/recipe constraints
        if (expectedPanOpt.isPresent()) {
            PanTypeComponent pan = playerStack.get(ModDataComponentTypes.PAN_TYPE.get());
            if (pan == null || !Objects.equals(pan.id(), expectedPanOpt.get().id())) return false;
        }
        if (expectedRecipeIdOpt.isPresent()) {
            if (!Objects.equals(actual.recipeId(), expectedRecipeIdOpt.get())) return false;
        }

        // Compare per-ingredient
        Map<String, Integer> want = toMap(expected);
        Map<String, Integer> have = toMap(actual);

        for (Map.Entry<String, Integer> e : want.entrySet()) {
            int got = have.getOrDefault(e.getKey(), 0);
            if (Math.abs(got - e.getValue()) > tol.perIngredientMg) return false;
        }

        // No substantial extras: anything not in "want" must be <= per-ingredient tolerance
        for (Map.Entry<String, Integer> e : have.entrySet()) {
            if (!want.containsKey(e.getKey()) && e.getValue() > tol.perIngredientMg) {
                return false;
            }
        }

        // Total weight tolerance (use mg derived from ingredients)
        int wantTotal = want.values().stream().mapToInt(Integer::intValue).sum();
        int haveTotal = have.values().stream().mapToInt(Integer::intValue).sum();
        return Math.abs(haveTotal - wantTotal) <= tol.totalMg;
    }

    /** Convenience to build a matcher from the **display** stack used in the trade UI. */
    public static java.util.function.Predicate<ItemStack> fromDisplay(ItemStack display, Tolerance tol) {
        DoughRecipeComponent expected = display.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        PanTypeComponent expectedPan = display.get(ModDataComponentTypes.PAN_TYPE.get());
        ResourceLocation expectedRecipeId = expected != null ? expected.recipeId() : null;

        return stack -> expected != null
                && matchesWithinTolerance(
                stack,
                expected,
                tol,
                Optional.ofNullable(expectedPan),
                Optional.ofNullable(expectedRecipeId)
        );
    }
}
