package net.boulangermod.boulanger.datagen.bread;

import net.boulangermod.boulanger.datagen.builder.DoughProcessRecipeBuilder;
import net.boulangermod.boulanger.datagen.builder.RatioRecipeBuilder;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class WholeWheatBreadRecipes {
    private WholeWheatBreadRecipes() {}
    public static void register(RecipeOutput out) {
        new RatioRecipeBuilder(
                DatagenIds.id("whole_wheat_bread"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D
        )
                .addComponent(IngredientCategory.FLOUR, 70.0,
                        List.of(DatagenIds.id("whole_wheat_flour")))
                .addComponent(IngredientCategory.FLOUR, 25.0,
                        List.of(DatagenIds.id("high_gluten_flour")))
                .addComponent(IngredientCategory.FLOUR, 5.0,
                        List.of(DatagenIds.id("wheat_bran")))
                .addComponent(IngredientCategory.SUGAR, 10.0,
                        List.of(DatagenIds.id("brown_sugar")))
                .addComponent(IngredientCategory.SALT, 6.0, List.of())
                .addComponent(IngredientCategory.YEAST, 8.0, List.of())
                .addComponent(IngredientCategory.EGGS, 10.0,
                        List.of(DatagenIds.id("fancy_egg")))
                .addComponent(IngredientCategory.WATER, 72.0,
                        List.of(DatagenIds.mc("water_bucket")))
                .servingWeight(680.0)
                .save(out);


        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/whole_wheat_bread"),
                DatagenIds.id("whole_wheat_bread")
        )
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.DIVIDE)
                .addStep(StepType.SHAPE)
                .addStep(StepType.PROOF, 1600)
                .setServingWeight(680.0)
                .setPanType(DatagenIds.pan(PanType.LOAF))
                .save(out);
    }
}