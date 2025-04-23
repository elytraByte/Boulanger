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

import static java.util.Map.of;

public class ModMixingRecipes {
    private static final List<MixingRecipe> RECIPES = new java.util.ArrayList<>();

    public static void registerDefaults() {
        // Baguette: 100% flour, 2% yeast, 2% salt
        RECIPES.add(new MixingRecipe(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette"),
                of(
                        IngredientCategory.FLOUR, 100.0,
                        IngredientCategory.WATER, 70.0,
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

                // 1) Baker’s %: flour is baseline 100%, then eggs/salt/sugar/yeast on top
                of(
                        IngredientCategory.FLOUR, 100.0,
                        IngredientCategory.EGGS,   10.0,
                        IngredientCategory.SALT,    4.0,
                        IngredientCategory.SUGAR,   4.0,
                        IngredientCategory.YEAST,   8.0
                ),

                ModItems.DOUGH.get(),

                // 2) allowed flour‐type IDs + other ingredients
                Set.of(
                        FlourItemType.WHOLE_WHEAT_FLOUR.getId(),
                        FlourItemType.HIGH_GLUTEN_FLOUR.getId(),   // used here for “bread flour”
                        FlourItemType.BRAN.getId(),
                        FoodAdditiveType.FANCY_EGG.getId(),
                        FoodAdditiveType.SALT.getId(),
                        FoodAdditiveType.BROWN_SUGAR.getId(),
                        FoodAdditiveType.SAF_RED_YEAST.getId()
                ),

                // 3) per‐item breakdown of the FLOUR category
                List.of(
                        new MixingRecipe.IngredientRequirement(
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                                        FlourItemType.WHOLE_WHEAT_FLOUR.getId()),
                                IngredientCategory.FLOUR,
                                75.0
                        ),
                        new MixingRecipe.IngredientRequirement(
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                                        FlourItemType.HIGH_GLUTEN_FLOUR.getId()),
                                IngredientCategory.FLOUR,
                                25.0
                        ),
                        new MixingRecipe.IngredientRequirement(
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                                        FlourItemType.BRAN.getId()),
                                IngredientCategory.FLOUR,
                                10.0
                        )
                )
        ));



        // … any other recipes …
    }

    public static List<MixingRecipe> getAll() {
        return RECIPES;
    }
}
