package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Map;

public record MixingRecipe(
        ResourceLocation id,
        Map<IngredientCategory, Double> targetPercentages,
        Item resultItem
) { }
