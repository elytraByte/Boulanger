package net.boulangermod.boulanger.datagen.bread;


import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.datagen.builder.DoughProcessRecipeBuilder;
import net.boulangermod.boulanger.datagen.builder.RatioRecipeBuilder;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;


import java.util.List;


/** Small helpers to avoid repeating boilerplate when building ResourceLocations. */
final class DatagenIds {
    private DatagenIds() {}
    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, path);
    }
    static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
    // DatagenIds.java
    static ResourceLocation pan(PanType pan) {
        // PanType#getId returns a fully qualified ID like "boulanger:baguette"
        ResourceLocation rl = ResourceLocation.tryParse(pan.getId());
        if (rl == null) throw new IllegalArgumentException("Bad PanType id: " + pan.getId());
        return rl; // -> "boulanger:baguette"
    }

}