package net.boulangermod.boulanger.content.ingredient;

import net.minecraft.world.item.Item;

public final class IngredientClassifier {
    private IngredientClassifier() {}

    public static IngredientCategory fromItem(Item item) {
        if (item.builtInRegistryHolder().is(IngredientTags.WATER))       return IngredientCategory.WATER;
        if (item.builtInRegistryHolder().is(IngredientTags.EGGS))        return IngredientCategory.EGGS;
        if (item.builtInRegistryHolder().is(IngredientTags.DAIRY))       return IngredientCategory.DAIRY;
        if (item.builtInRegistryHolder().is(IngredientTags.SALT))        return IngredientCategory.SALT;
        if (item.builtInRegistryHolder().is(IngredientTags.YEAST))       return IngredientCategory.YEAST;
        if (item.builtInRegistryHolder().is(IngredientTags.FAT))         return IngredientCategory.FAT;
        if (item.builtInRegistryHolder().is(IngredientTags.SUGAR))       return IngredientCategory.SUGAR;
        if (item.builtInRegistryHolder().is(IngredientTags.ADDITIVE))    return IngredientCategory.ADDITIVE;
        if (item.builtInRegistryHolder().is(IngredientTags.ENRICHMENT))  return IngredientCategory.ENRICHMENT;
        return IngredientCategory.CUSTOM;
    }
}
