package net.boulangermod.boulanger.datagen.bread;

import net.minecraft.data.recipes.RecipeOutput;

public final class BreadRecipes {
    private BreadRecipes() {}
    public static void registerAll(RecipeOutput out) {
        BaguetteRecipes.register(out);
//        WholeWheatBreadRecipes.register(out);
//        BanhMiRecipes.register(out);
//        BriocheRecipes.register(out);
//        WhitePanBreadRecipes.register(out);
    }
}
