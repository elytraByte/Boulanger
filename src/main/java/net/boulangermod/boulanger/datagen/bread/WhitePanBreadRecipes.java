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

public final class WhitePanBreadRecipes {
    private WhitePanBreadRecipes() {}

    public static void register(RecipeOutput out) {
        new RatioRecipeBuilder(
                DatagenIds.id("white_pan_bread"),
                new ItemStack(ModItems.DOUGH.get()),
                0.05D
        )
                .addComponent(IngredientCategory.FLOUR, 90,
                        List.of(DatagenIds.id("bread_flour")))
                .addComponent(IngredientCategory.FLOUR, 10,
                        List.of(DatagenIds.id("vital_wheat_gluten")))
                .addComponent(IngredientCategory.WATER, 60.5,
                        List.of(DatagenIds.mc("water_bucket")))
                .addComponent(IngredientCategory.DAIRY, 5.0,
                        List.of(DatagenIds.id("whole_milk")))
                .addComponent(IngredientCategory.YEAST, 6.0, List.of())
                .addComponent(IngredientCategory.SUGAR, 4, List.of())
                .addComponent(IngredientCategory.FAT, 15.0,
                        List.of(DatagenIds.id("butter")))
                .addComponent(IngredientCategory.ADDITIVE, 0.01,
                        List.of(DatagenIds.id("ascorbic_acid")))
                .addComponent(IngredientCategory.ADDITIVE, 0.110,
                        List.of(DatagenIds.id("calcium_propionate")))
                .addComponent(IngredientCategory.ADDITIVE, 0.110,
                        List.of(DatagenIds.id("diastatic_malt_powder")))
                .addComponent(IngredientCategory.ADDITIVE, 0.2,
                        List.of(DatagenIds.id("l_cysteine")))
                .rollSizeG(65)   // roll size 65 g
                .loafSizeG(680)  // loaf size 680 g
                .save(out);

        new DoughProcessRecipeBuilder(
                DatagenIds.id("dough_process/white_pan_bread")
        )
                .proof(4)
                .punchdown()
                .proof(4)
                .punchdown()
                .divide()
                .shape()
                .proof(5)
                // Pan enforcement per portion
                .serve(PortionKind.ROLL, 65, PanType.BAGUETTE, 12) // rolls use baguette pan (12 per pan)
                .serve(PortionKind.LOAF, 680, PanType.LOAF, 1)     // loaf uses loaf pan (1 per pan)
                .save(out);
    }
}
