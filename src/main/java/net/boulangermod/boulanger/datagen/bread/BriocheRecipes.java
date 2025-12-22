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

public final class BriocheRecipes {
    private BriocheRecipes() {}

    public static void register(RecipeOutput out) {
        // Composition + serving sizes
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
                .rollSizeG(65)     // 65 g roll
                .loafSizeG(680)    // 680 g loaf
                .save(out);

        // Process: proof 3m, punch, proof 3m, punch, divide, shape, proof 4m
        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/brioche")
        )
                .proof(3)
                .punchdown()
                .proof(3)
                .punchdown()
                .divide()
                .shape()
                .proof(4)
                // Per-portion pan rules
                .serve(PortionKind.ROLL, 65, PanType.LOAF, 12) // 12 rolls per pan
                .serve(PortionKind.LOAF, 680, PanType.LOAF, 1) // 1 loaf per pan
                .save(out);
    }
}
