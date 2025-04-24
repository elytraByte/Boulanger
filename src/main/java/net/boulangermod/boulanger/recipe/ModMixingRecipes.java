package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.FoodAdditiveType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
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
                        IngredientCategory.WATER, 70.0,
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

        // White Pan Bread: 100% bread flour (high-gluten), 67% water, 2% yeast, 6% sugar, 3% salt
        RECIPES.add(new MixingRecipe(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "white_pan_bread"),
                of(
                        IngredientCategory.FLOUR, 100.0,
                        IngredientCategory.WATER, 67.0,
                        IngredientCategory.YEAST,   2.0,
                        IngredientCategory.SUGAR,   6.0,
                        IngredientCategory.SALT,    3.0
                ),
                ModItems.DOUGH.get(),
                // only bread (high-gluten) flour, yeast, sugar, and salt allowed
                Set.of(
                        FlourItemType.BREAD_FLOUR.getId(),
                        FoodAdditiveType.SAF_RED_YEAST.getId(),
                        FoodAdditiveType.BROWN_SUGAR.getId(),
                        FoodAdditiveType.SALT.getId()
                )
        ));

        // Banh Mi: 50% bread flour, 50% high-gluten flour, 15% eggs, 30% water, 30% milk, 5% butter, 5% yeast, 3% sugar
        RECIPES.add(new MixingRecipe(
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "banh_mi"),
                of(
                        IngredientCategory.FLOUR, 100.0,
                        IngredientCategory.EGGS,   15.0,
                        IngredientCategory.WATER,  30.0,
                        IngredientCategory.DAIRY,   30.0,
                        IngredientCategory.FAT, 5.0,
                        IngredientCategory.YEAST,  5.0,
                        IngredientCategory.SUGAR,  3.0
                ),
                ModItems.DOUGH.get(),
                // whitelist ingredient IDs
                Set.of(
                        FlourItemType.BREAD_FLOUR.getId(),  // custom type for bread flour
                        FlourItemType.HIGH_GLUTEN_FLOUR.getId(),
                        FoodAdditiveType.FANCY_EGG.getId(),
                        FoodAdditiveType.SAF_RED_YEAST.getId(),
                        FoodAdditiveType.BUTTER.getId(),
                        FoodAdditiveType.WHOLE_MILK.getId(),
                        FoodAdditiveType.BROWN_SUGAR.getId(),
                        FoodAdditiveType.SALT.getId()
                ),
                // breakdown of the FLOUR category
                List.of(
                        new MixingRecipe.IngredientRequirement(
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                                        FlourItemType.BREAD_FLOUR.getId()),
                                IngredientCategory.FLOUR,
                                50.0
                        ),
                        new MixingRecipe.IngredientRequirement(
                                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                                        FlourItemType.HIGH_GLUTEN_FLOUR.getId()),
                                IngredientCategory.FLOUR,
                                50.0
                        )
                )
        ));



        // … any other recipes …
    }

    public static List<MixingRecipe> getAll() {
        return RECIPES;
    }
}
