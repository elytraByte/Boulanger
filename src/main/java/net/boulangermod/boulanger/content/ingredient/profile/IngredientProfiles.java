package net.boulangermod.boulanger.content.ingredient.profile;

import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.content.ingredient.IngredientClassifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class IngredientProfiles {
    private static final Logger LOG = LogManager.getLogger();

    private IngredientProfiles() {}

    private static volatile Map<ResourceLocation, IngredientProfile> BY_ID = Map.of();
    private static final List<IngredientIdResolver> ID_RESOLVERS = new CopyOnWriteArrayList<>();

    public static void registerIdResolver(IngredientIdResolver resolver) {
        ID_RESOLVERS.add(resolver);
    }

    public static void replaceAll(Map<ResourceLocation, IngredientProfile> newMap) {
        BY_ID = Map.copyOf(newMap);
        LOG.info("Loaded {} IngredientProfiles", BY_ID.size());
    }

    public static ResourceLocation ingredientIdOf(ItemStack stack) {
        for (IngredientIdResolver r : ID_RESOLVERS) {
            ResourceLocation id = r.resolve(stack);
            if (id != null) return id;
        }
        // Fallback: use the item registry key
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static IngredientProfile profileOf(ItemStack stack) {
        ResourceLocation id = ingredientIdOf(stack);
        IngredientProfile p = BY_ID.get(id);
        if (p != null) return p;

        // Fallback: derive a minimal profile from tags/classifier
        IngredientCategory cat = IngredientClassifier.fromItem(stack.getItem());
        return defaultProfile(id, cat);
    }

    private static IngredientProfile defaultProfile(ResourceLocation id, IngredientCategory cat) {
        // Conservative defaults: only "pure" categories get fractions.
        double water = 0.0, fat = 0.0, sugar = 0.0, salt = 0.0;

        switch (cat) {
            case WATER -> water = 1.0;
            case FAT -> fat = 1.0;
            case SUGAR -> sugar = 1.0;
            case SALT -> salt = 1.0;
            default -> { /* leave zeros */ }
        }

        return new IngredientProfile(
                id,
                cat,
                water, fat, sugar, salt,
                null,
                null,
                List.of()
        );
    }
}
