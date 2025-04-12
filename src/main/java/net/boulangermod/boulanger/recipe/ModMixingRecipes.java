package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;     // ← NEW

public class ModMixingRecipes {
    private static final List<MixingRecipe> RECIPES = new ArrayList<>();

    public static void registerDefaults() {
        /* ------------------------------------------------------------------
         * Baguette dough
         * – 100 % flour, 50 % fat
         * – Flour must be whole‑wheat or high‑gluten (or any mix of the two)
         * ---------------------------------------------------------------- */
        RECIPES.add(new MixingRecipe(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette"),
                Map.of(IngredientCategory.FLOUR, 100.0, IngredientCategory.FAT, 50.0),
                ModItems.DOUGH.get(),
                /* flour‑type whitelist (by ID) */
                Set.of(
                        FlourItemType.WHOLE_WHEAT_FLOUR.getId(),
                        FlourItemType.HIGH_GLUTEN_FLOUR.getId()
                )
        ));

        /* ------------------------------------------------------------------
         * Example of a recipe that accepts *any* flour:
         * pass Set.of() (empty set) to skip the whitelist check.
         * ---------------------------------------------------------------- */
        // RECIPES.add(new MixingRecipe(
        //         new ResourceLocation(Boulanger.MODID, "generic_dough"),
        //         Map.of(
        //                 IngredientCategory.FLOUR, 100.0,
        //                 IngredientCategory.WATER, 65.0
        //         ),
        //         ModItems.DOUGH.get(),
        //         Set.of()                 // ← no flour restriction
        // ));
    }

    public static List<MixingRecipe> getAll() {
        return RECIPES;
    }
}
