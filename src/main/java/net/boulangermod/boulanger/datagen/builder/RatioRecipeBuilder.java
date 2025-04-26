package net.boulangermod.boulanger.datagen.builder;

import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class RatioRecipeBuilder {
    private final ResourceLocation id;
    private final ItemStack result;
    private final Map<IngredientCategory, Double> targets;
    private final double tolerance;
    private final Map<IngredientCategory, List<ResourceLocation>> allowedItems;

    /** 4-arg constructor: no whitelist (allowedItems = empty map) */
    public RatioRecipeBuilder(ResourceLocation id,
                              ItemStack result,
                              Map<IngredientCategory, Double> targets,
                              double tolerance) {
        this(id, result, targets, tolerance, Map.of());
    }

    /** 5-arg constructor: supply your per-category whitelist */
    public RatioRecipeBuilder(ResourceLocation id,
                              ItemStack result,
                              Map<IngredientCategory, Double> targets,
                              double tolerance,
                              Map<IngredientCategory, List<ResourceLocation>> allowedItems) {
        this.id            = id;
        this.result        = result;
        this.targets       = targets;
        this.tolerance     = tolerance;
        this.allowedItems  = allowedItems != null ? allowedItems : Map.of();
    }

    public void save(RecipeOutput output) {
        // build your RatioRecipe with an explicit (possibly empty) allowedItems map
        var recipe = new RatioRecipe(
                id,
                targets,
                tolerance,
                result,
                List.of(),      // requirements for now
                allowedItems    // non-null Map<IngredientCategory,List<RL>>
        );
        output.accept(id, recipe, null);
    }
}