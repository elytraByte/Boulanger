package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModMixingRecipes {
    private static final List<MixingRecipe> RECIPES = new ArrayList<>();

    public static void registerDefaults() {
        // Baguette dough: 100% flour, 65% water, 2% salt, 1% yeast
        RECIPES.add(new MixingRecipe(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette"),
                Map.of(
                        IngredientCategory.FLOUR, 100.0,
                       IngredientCategory.FAT, 50.0
//                        IngredientCategory.SALT, 2.0,
//                        IngredientCategory.YEAST, 1.0
                ),
                ModItems.DOUGH.get()
        ));

//        // Pizza dough: 100% flour, 60% water, 2% salt, 0.5% yeast, 2% olive_oil
//        RECIPES.add(new MixingRecipe(
//                new ResourceLocation(Boulanger.MODID, "pizza"),
//                Map.of(
//                        IngredientCategory.FLOUR, 100.0,
//                        IngredientCategory.WATER, 60.0,
//                        IngredientCategory.SALT, 2.0,
//                        IngredientCategory.YEAST, 0.5,
//                        IngredientCategory.OIL, 2.0
//                ),
//                ModItems.PIZZA_DOUGH.get()
//        ));

        // … add more recipes here
    }

    public static List<MixingRecipe> getAll() {
        return RECIPES;
    }
}
