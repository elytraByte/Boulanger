package net.boulangermod.boulanger.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.component.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public enum IngredientCategory {
    FLOUR,
    LIQUID,
    SALT,
    YEAST,
    FAT,
    SUGAR,
    ADDITIVE,
    ENRICHMENT,
    CUSTOM;

    public static IngredientCategory getIngredientCategory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return IngredientCategory.CUSTOM;

        // Check if the ItemStack has a FoodAdditiveComponent.
        if (stack.has(ModDataComponentTypes.FOOD_ADDITIVE.get())) {
            FoodAdditiveComponent additive = stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
            if (additive != null) {
                String id = additive.getId();
                // Map known food additive IDs to FAT, if applicable.
                if ("butter".equals(id) || "butter_salted".equals(id) ||
                        "european_butter".equals(id) || "european_butter_salted".equals(id) ||
                        "european_butter_blend".equals(id) || "salted_european_butter_blend".equals(id)) {
                    return IngredientCategory.FAT;
                }
                // Otherwise, return a generic ADDITIVE category.
                return IngredientCategory.ADDITIVE;
            }
        }

        // Check if the item has a FlourType component.
        if (stack.has(ModDataComponentTypes.FLOUR_TYPE.get())) {
            return IngredientCategory.FLOUR;
        }

        // Use item tags.
        if (stack.is(IngredientTags.LIQUIDS)) return IngredientCategory.LIQUID;
        if (stack.is(IngredientTags.SALTS)) return IngredientCategory.SALT;
        if (stack.is(IngredientTags.YEASTS)) return IngredientCategory.YEAST;
        if (stack.is(IngredientTags.FATS)) return IngredientCategory.FAT;
        if (stack.is(IngredientTags.SUGARS)) return IngredientCategory.SUGAR;
        if (stack.is(IngredientTags.ADDITIVES)) return IngredientCategory.ADDITIVE;
        if (stack.is(IngredientTags.ENRICHMENTS)) return IngredientCategory.ENRICHMENT;

        // Fallback
        return IngredientCategory.CUSTOM;
    }

    // A simple Codec (for JSON or config persistence) converting using strings.
    public static final Codec<IngredientCategory> CODEC =
            Codec.STRING.xmap(IngredientCategory::valueOf, IngredientCategory::name);

    /**
     * A StreamCodec that encodes/decodes an IngredientCategory to/from a RegistryFriendlyByteBuf by writing its ordinal.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientCategory> STREAM_CODEC =
            new StreamCodec<RegistryFriendlyByteBuf, IngredientCategory>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, IngredientCategory value) {
                    // Write the enum's ordinal as an int.
                    ByteBufCodecs.INT.encode(buffer, value.ordinal());
                }

                @Override
                public IngredientCategory decode(RegistryFriendlyByteBuf buffer) {
                    int ordinal = ByteBufCodecs.INT.decode(buffer);
                    IngredientCategory[] values = IngredientCategory.values();
                    return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : CUSTOM;
                }
            };

    /**
     * Returns a StreamCodec for a List of IngredientCategory using RegistryFriendlyByteBuf.
     */
    public static StreamCodec<RegistryFriendlyByteBuf, List<IngredientCategory>> listOf() {
        return new StreamCodec<RegistryFriendlyByteBuf, List<IngredientCategory>>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buffer, List<IngredientCategory> list) {
                // Write the list size as an int.
                ByteBufCodecs.INT.encode(buffer, list.size());
                // Encode each IngredientCategory using the STREAM_CODEC defined above.
                for (IngredientCategory cat : list) {
                    STREAM_CODEC.encode(buffer, cat);
                }
            }

            @Override
            public List<IngredientCategory> decode(RegistryFriendlyByteBuf buffer) {
                int size = ByteBufCodecs.INT.decode(buffer);
                List<IngredientCategory> result = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    result.add(STREAM_CODEC.decode(buffer));
                }
                return result;
            }
        };
    }
}
