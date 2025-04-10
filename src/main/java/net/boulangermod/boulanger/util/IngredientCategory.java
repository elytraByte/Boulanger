package net.boulangermod.boulanger.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

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

        // 1. If it has a FlourType component
        if (stack.has(ModDataComponentTypes.FLOUR_TYPE.get())) {
            return IngredientCategory.FLOUR;
        }

        // 2. Check item tags
        if (stack.is(IngredientTags.LIQUIDS)) return IngredientCategory.LIQUID;
        if (stack.is(IngredientTags.SALTS)) return IngredientCategory.SALT;
        if (stack.is(IngredientTags.YEASTS)) return IngredientCategory.YEAST;
        if (stack.is(IngredientTags.FATS)) return IngredientCategory.FAT;
        if (stack.is(IngredientTags.SUGARS)) return IngredientCategory.SUGAR;
        if (stack.is(IngredientTags.ADDITIVES)) return IngredientCategory.ADDITIVE;
        if (stack.is(IngredientTags.ENRICHMENTS)) return IngredientCategory.ENRICHMENT;

        // 3. Fallback
        return IngredientCategory.CUSTOM;
    }

    public static final Codec<IngredientCategory> CODEC =
            Codec.STRING.xmap(IngredientCategory::valueOf, IngredientCategory::name);

    public static final StreamCodec<ByteBuf, IngredientCategory> STREAM_CODEC =
            ByteBufCodecs.idMapper(
                    id -> id >= 0 && id < IngredientCategory.values().length ? IngredientCategory.values()[id] : IngredientCategory.CUSTOM,
                    IngredientCategory::ordinal
            );
}


