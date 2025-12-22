package net.boulangermod.boulanger.datagen.bread;

import net.boulangermod.boulanger.datagen.builder.DoughProcessRecipeBuilder;
import net.boulangermod.boulanger.datagen.builder.RatioRecipeBuilder;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.item.PortionKind;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class BanhMiRecipes {
    private BanhMiRecipes() {}

    public static void register(RecipeOutput out) {
        // Composition + serving size (rolls only: 180 g)
        new RatioRecipeBuilder(
                DatagenIds.id("banh_mi"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D
        )
                .addComponent(IngredientCategory.FLOUR, 57.0, List.of(DatagenIds.id("bread_flour")))
                .addComponent(IngredientCategory.FLOUR, 43.0, List.of(DatagenIds.id("high_gluten_flour")))
                .addComponent(IngredientCategory.WATER, 31.0, List.of(DatagenIds.mc("water_bucket")))
                .addComponent(IngredientCategory.DAIRY, 31.0, List.of(DatagenIds.id("whole_milk")))
                .addComponent(IngredientCategory.EGGS, 12.0, List.of(DatagenIds.id("fancy_egg")))
                .addComponent(IngredientCategory.FAT, 4.0, List.of(DatagenIds.id("butter")))
                .addComponent(IngredientCategory.SUGAR, 3.0, List.of(DatagenIds.id("brown_sugar")))
                .addComponent(IngredientCategory.SALT, 2.0, List.of())
                .addComponent(IngredientCategory.ADDITIVE, 0.09, List.of(DatagenIds.id("ascorbic_acid")))
                .addComponent(IngredientCategory.ADDITIVE, 0.03, List.of(DatagenIds.id("calcium_propionate")))
                .addComponent(IngredientCategory.ADDITIVE, 0.005, List.of(DatagenIds.id("diastatic_malt_powder")))
                .addComponent(IngredientCategory.ADDITIVE, 0.005, List.of(DatagenIds.id("l_cysteine")))
                .rollSizeG(180) // roll portion only
                .save(out);

        // Process: proof 3, punchdown, proof 3, punchdown, divide, shape, proof 3
        new DoughProcessRecipeBuilder(DatagenIds.id("dough_process/banh_mi"))
                .proof(3)
                .punchdown()
                .proof(3)
                .punchdown()
                .divide()
                .shape()
                .proof(3)

                // Rolls (banh mi) go on a baguette pan, 6 per pan
                .serve(PortionKind.ROLL, 180, PanType.BAGUETTE, 6)

                .save(out);
    }
}
