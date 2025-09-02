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
        // Ratio: keep composition + expose BOTH serving sizes
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
                .rollSizeG(180)   // demi/roll-sized baguette
                .loafSizeG(454)   // full-size baguette
                .save(out);

        // Single dough-process (method is identical regardless of size)
        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/baguette"),
                DatagenIds.id("baguette")
        )
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.DIVIDE) // no fixed serving weight here
                .addStep(StepType.SHAPE)
                .setPanType(DatagenIds.pan(PanType.BAGUETTE))
                .save(out);
    }
}
