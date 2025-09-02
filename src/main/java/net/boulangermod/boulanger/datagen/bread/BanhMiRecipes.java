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

public final class BanhMiRecipes {
    private BanhMiRecipes() {}

    public static void register(RecipeOutput out) {
        new RatioRecipeBuilder(
                DatagenIds.id("banh_mi"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D
        )
                .addComponent(IngredientCategory.FLOUR, 57.0,
                        List.of(DatagenIds.id("bread_flour")))
                .addComponent(IngredientCategory.FLOUR, 43.0,
                        List.of(DatagenIds.id("high_gluten_flour")))
                .addComponent(IngredientCategory.WATER, 31.0,
                        List.of(DatagenIds.mc("water_bucket")))
                .addComponent(IngredientCategory.DAIRY, 31.0,
                        List.of(DatagenIds.id("whole_milk")))
                .addComponent(IngredientCategory.EGGS, 12.0,
                        List.of(DatagenIds.id("fancy_egg")))
                .addComponent(IngredientCategory.FAT, 4.0,
                        List.of(DatagenIds.id("butter")))
                .addComponent(IngredientCategory.SUGAR, 3.0,
                        List.of(DatagenIds.id("brown_sugar")))
                .addComponent(IngredientCategory.SALT, 2.0, List.of())
                .addComponent(IngredientCategory.ADDITIVE, 0.09,
                        List.of(DatagenIds.id("ascorbic_acid")))
                .addComponent(IngredientCategory.ADDITIVE, 0.03,
                        List.of(DatagenIds.id("calcium_propionate")))
                .addComponent(IngredientCategory.ADDITIVE, 0.005,
                        List.of(DatagenIds.id("diastatic_malt_powder")))
                .addComponent(IngredientCategory.ADDITIVE, 0.005,
                        List.of(DatagenIds.id("l_cysteine")))
                .loafSizeG(180) // only a loaf size; no roll size
                .save(out);

        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/banh_mi"),
                DatagenIds.id("banh_mi")
        )
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.PROOF, 1600)
                .addStep(StepType.PUNCHDOWN)
                .addStep(StepType.DIVIDE) // serving weight omitted (optional)
                .addStep(StepType.SHAPE)
                .setPanType(DatagenIds.pan(PanType.BAGUETTE))
                .addStep(StepType.PROOF, 1600)
                .save(out);
    }
}
