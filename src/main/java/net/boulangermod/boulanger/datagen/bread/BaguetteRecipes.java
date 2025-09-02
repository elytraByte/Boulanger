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

public final class BaguetteRecipes {
    private BaguetteRecipes() {}
    public static void register(RecipeOutput out) {
        new RatioRecipeBuilder(
                DatagenIds.id("baguette"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D
        )
                .addComponent(IngredientCategory.FLOUR, 95.0,
                        List.of(DatagenIds.id("bread_flour")))
                .addComponent(IngredientCategory.FLOUR, 5.0,
                        List.of(DatagenIds.id("vital_wheat_gluten")))
                .addComponent(IngredientCategory.WATER, 70.0,
                        List.of(DatagenIds.mc("water_bucket")))
                .addComponent(IngredientCategory.SALT, 3.0, List.of())
                .addComponent(IngredientCategory.YEAST, 4.0, List.of())
                .servingWeight(454)
                .save(out);


        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/baguette"),
                DatagenIds.id("baguette")
        )
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.DIVIDE)
                .setServingWeight(454.0)
                .addStep(StepType.SHAPE)
                .setPanType(DatagenIds.pan(PanType.BAGUETTE))
                .save(out);
    }
}
