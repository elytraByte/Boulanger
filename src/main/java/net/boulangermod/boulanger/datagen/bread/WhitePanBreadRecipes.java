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

public final class WhitePanBreadRecipes {
    private WhitePanBreadRecipes() {}
    public static void register(RecipeOutput out) {
        new RatioRecipeBuilder(
                DatagenIds.id("white_pan_bread"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D
        )
                .addComponent(IngredientCategory.FLOUR, 90.08,
                        List.of(DatagenIds.id("bread_flour")))
                .addComponent(IngredientCategory.FLOUR, 9.92,
                        List.of(DatagenIds.id("vital_wheat_gluten")))
                .addComponent(IngredientCategory.WATER, 60.52,
                        List.of(DatagenIds.mc("water_bucket")))
                .addComponent(IngredientCategory.DAIRY, 4.56,
                        List.of(DatagenIds.id("whole_milk")))
                .addComponent(IngredientCategory.YEAST, 5.95, List.of())
                .addComponent(IngredientCategory.SUGAR, 4.37, List.of())
                .addComponent(IngredientCategory.FAT, 14.88,
                        List.of(DatagenIds.id("butter")))
                .addComponent(IngredientCategory.ADDITIVE, 0.099,
                        List.of(DatagenIds.id("ascorbic_acid")))
                .addComponent(IngredientCategory.ADDITIVE, 0.1091,
                        List.of(DatagenIds.id("calcium_propionate")))
                .addComponent(IngredientCategory.ADDITIVE, 0.1091,
                        List.of(DatagenIds.id("diastatic_malt_powder")))
                .addComponent(IngredientCategory.ADDITIVE, 0.1984,
                        List.of(DatagenIds.id("l_cysteine")))
                .servingWeight(680.0)
                .save(out);


        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/white_pan_bread"),
                DatagenIds.id("white_pan_bread")
        )
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.DIVIDE)
                .setServingWeight(680.0)
                .addStep(StepType.SHAPE)
                .setPanType(DatagenIds.pan(PanType.LOAF))
                .addStep(StepType.PROOF, 800)
                .save(out);
    }
}