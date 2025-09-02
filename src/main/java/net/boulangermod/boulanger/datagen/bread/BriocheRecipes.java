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

public final class BriocheRecipes {
    private BriocheRecipes() {}
    public static void register(RecipeOutput out) {
        new RatioRecipeBuilder(
                DatagenIds.id("brioche"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D
        )
                .addComponent(IngredientCategory.FLOUR, 100.0,
                        List.of(DatagenIds.id("all_purpose_flour")))
                .addComponent(IngredientCategory.EGGS, 57.69,
                        List.of(DatagenIds.id("fancy_egg")))
                .addComponent(IngredientCategory.FAT, 43.65,
                        List.of(DatagenIds.id("butter")))
                .addComponent(IngredientCategory.DAIRY, 10.96,
                        List.of(DatagenIds.id("whole_milk")))
                .addComponent(IngredientCategory.SUGAR, 9.62, List.of())
                .addComponent(IngredientCategory.SALT, 1.15, List.of())
                .addComponent(IngredientCategory.YEAST, 0.58, List.of())
                .addComponent(IngredientCategory.ADDITIVE, 0.31,
                        List.of(DatagenIds.id("calcium_propionate")))
                .addComponent(IngredientCategory.ADDITIVE, 0.03,
                        List.of(DatagenIds.id("ascorbic_acid")))
                .rollSizeG(65)    // brioche roll
                .loafSizeG(680)   // brioche loaf
                .save(out);

        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/brioche"),
                DatagenIds.id("brioche")
        )
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.DIVIDE) // serving weight omitted (optional)
                .addStep(StepType.SHAPE)
                .setPanType(DatagenIds.pan(PanType.LOAF))
                .addStep(StepType.PROOF, 800)
                .save(out);
    }
}
