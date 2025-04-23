package net.boulangermod.boulanger.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class IngredientTags {
    public static final TagKey<Item> WATER = tag("water");
    public static final TagKey<Item> EGGS = tag("eggs");
    public static final TagKey<Item> DAIRY = tag("dairy");
    public static final TagKey<Item> SALTS = tag("salts");
    public static final TagKey<Item> YEASTS = tag("yeasts");
    public static final TagKey<Item> FATS = tag("fats");
    public static final TagKey<Item> SUGARS = tag("sugars");
    public static final TagKey<Item> ADDITIVES = tag("additives");
    public static final TagKey<Item> ENRICHMENTS = tag("enrichments");

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("boulanger", name));
    }

}
