package net.boulangermod.boulanger.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.boulangermod.boulanger.util.IngredientCategory;
import java.util.Map;

public class MixingRecipe {
    private final ResourceLocation id;
    private final Map<IngredientCategory, Double> targetPercentages;
    private final Item resultItem;

    public MixingRecipe(ResourceLocation id, Map<IngredientCategory, Double> targetPercentages, Item resultItem) {
        this.id = id;
        this.targetPercentages = targetPercentages;
        this.resultItem = resultItem;
    }

    /**
     * Returns the recipe's identifier.
     */
    public ResourceLocation getId() {
        return id;
    }

    /**
     * Returns the target baker's percentages for the recipe.
     */
    public Map<IngredientCategory, Double> targetPercentages() {
        return targetPercentages;
    }

    /**
     * Returns the result item for the recipe.
     */
    public Item resultItem() {
        return resultItem;
    }

    @Override
    public String toString() {
        return "MixingRecipe[id=" + id + ", targetPercentages=" + targetPercentages + ", resultItem=" + resultItem + "]";
    }
}
