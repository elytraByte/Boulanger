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
                        IngredientCategory.LIQUID, 70.0,
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

        RECIPES.add(new MixingRecipe(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "whole_wheat_bread"),

                // category‐only baker’s % (non‐flour)
                Map.of(
                        IngredientCategory.LIQUID, 70.0,
                        IngredientCategory.YEAST,   2.0,
                        IngredientCategory.SALT,    2.0
                ),

                ModItems.DOUGH.get(),

                // whitelist allowed flours + yeast IDs
                Set.of(
                        FlourItemType.WHOLE_WHEAT_FLOUR.getId(),
                        FlourItemType.BRAN.getId(),
                        FlourItemType.VITAL_WHEAT_GLUTEN.getId(),
                        FoodAdditiveType.SAF_RED_YEAST.getId()
                ),

                // **per‑item requirements** for FLOUR:
                List.of(
                        new RatioRecipe.IngredientRequirement(
                                // wrap the string ID into a ResourceLocation
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, FlourItemType.WHOLE_WHEAT_FLOUR.getId()),
                                IngredientCategory.FLOUR,
                                100.0
                        ),
                        new RatioRecipe.IngredientRequirement(
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, FlourItemType.BRAN.getId()),
                                IngredientCategory.FLOUR,
                                25.0
                        ),
                        new RatioRecipe.IngredientRequirement(
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, FlourItemType.VITAL_WHEAT_GLUTEN.getId()),
                                IngredientCategory.FLOUR,
                                25.0
                        )
                )
        ));


        // … any other recipes …
    }

    public static List<MixingRecipe> getAll() {
        return RECIPES;
    }
}
