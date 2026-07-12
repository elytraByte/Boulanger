package net.boulangermod.boulanger.content.ingredient;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class IngredientTags {
    public static final TagKey<Item> WATER = tag("ingredients/water");
    public static final TagKey<Item> FLOUR = tag("ingredients/flour");
    public static final TagKey<Item> EGGS = tag("ingredients/eggs");
    public static final TagKey<Item> DAIRY = tag("ingredients/dairy");
    public static final TagKey<Item> SALT = tag("ingredients/salt");
    public static final TagKey<Item> YEAST = tag("ingredients/yeast");
    public static final TagKey<Item> FAT = tag("ingredients/fat");
    public static final TagKey<Item> SUGAR = tag("ingredients/sugar");
    public static final TagKey<Item> ADDITIVE = tag("ingredients/additives");
    public static final TagKey<Item> ENRICHMENT = tag("ingredients/enrichments");


    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("boulanger", name));
    }

}