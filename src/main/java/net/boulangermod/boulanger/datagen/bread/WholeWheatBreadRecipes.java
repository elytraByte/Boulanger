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
                .rollSizeG(75)      // roll size 75 g
                .loafSizeG(680)     // loaf size 680 g
                .save(out);

        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/whole_wheat_bread")
        )
                .proof(5)
                .punchdown()
                .proof(5)
                .punchdown()
                .divide()
                .shape()
                .proof(5)
                // Per-portion pan enforcement & capacities
                .serve(PortionKind.ROLL, 75, PanType.BAGUETTE, 12) // rolls on baguette pan (12 per pan)
                .serve(PortionKind.LOAF, 680, PanType.LOAF, 1)     // loaf on loaf pan (1 per pan)
                .save(out);
    }
}
