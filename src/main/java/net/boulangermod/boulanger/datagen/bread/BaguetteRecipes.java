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

public final class BaguetteRecipes {
    private BaguetteRecipes() {}

    public static void register(RecipeOutput out) {

        var baseId    = DatagenIds.id("baguette");
        var processId = DatagenIds.id("baguette_process");
        // --- Ratio recipe: composition + two serving sizes ---
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

// Dough-process recipe: steps + per-portion pan rules (both use BAGUETTE pan)
        new DoughProcessRecipeBuilder(DatagenIds.id("dough_process/baguette"))
                .proof(2.5)
                .punchdown()
                .proof(2.5)
                .punchdown()
                .divide()
                .shape()
                .proof(3.0)

                // Use BAGUETTE pan for both sizes
                .serve(PortionKind.LOAF, 454, PanType.BAGUETTE, 3)  // 3 full baguettes / pan
                .serve(PortionKind.ROLL, 180, PanType.BAGUETTE, 6)  // 6 demi baguettes / pan

                .save(out);
    }
}
