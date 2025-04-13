package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.FoodAdditiveType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModMixingRecipes {
    private static final List<MixingRecipe> RECIPES = new java.util.ArrayList<>();

    public static void registerDefaults() {
        // Baguette: 100% flour, 2% yeast, 2% salt
        RECIPES.add(new MixingRecipe(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette"),
                Map.of(
                        IngredientCategory.FLOUR, 100.0,
                        IngredientCategory.YEAST,   2.0,
                        IngredientCategory.SALT,    2.0
                ),
                ModItems.DOUGH.get(),

                // whitelist by ID: only high‐gluten flour & saf_red yeast
                Set.of(
                        FlourItemType.HIGH_GLUTEN_FLOUR.getId(),
                        FoodAdditiveType.SAF_RED_YEAST.getId()
                )
        ));

        // … any other recipes …
    }

    public static List<MixingRecipe> getAll() {
        return RECIPES;
    }
}
