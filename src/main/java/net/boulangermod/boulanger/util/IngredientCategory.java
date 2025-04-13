package net.boulangermod.boulanger.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.component.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.FoodAdditiveType;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
        if (stack == null || stack.isEmpty()) {
            return CUSTOM;
        }

        // 1) If it has a FoodAdditiveComponent, map specific IDs to SALT or YEAST
        if (stack.has(ModDataComponentTypes.FOOD_ADDITIVE.get())) {
            FoodAdditiveComponent additive = stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
            if (additive != null) {
                String id = additive.getId();
                // Yeasts
                if (id.equals(FoodAdditiveType.SAF_RED_YEAST.getId())
                        || id.equals(FoodAdditiveType.FRESH_YEAST.getId())) {
                    return YEAST;
                }
//                // Salts
//                if (id.equals(FoodAdditiveType.SALT_KOSHER.getId())
//                        || id.equals(FoodAdditiveType.SALT_SEA.getId())
//                        || id.equals(FoodAdditiveType.SALT_TABLE.getId())) {
//                    return SALT;
//                }
//                // Fats
//                if (id.equals(FoodAdditiveType.BUTTER.getId())
//                        || id.equals(FoodAdditiveType.EUROPEAN_BUTTER.getId())) {
//                    return FAT;
//                }
//                // Sugars
//                if (id.equals(FoodAdditiveType.SUGAR_WHITE.getId())
//                        || id.equals(FoodAdditiveType.SUGAR_BROWN.getId())) {
//                    return SUGAR;
//                }
//                // Enrichments (e.g. milk powder, eggs)
//                if (id.equals(FoodAdditiveType.MILK_POWDER.getId())
//                        || id.equals(FoodAdditiveType.EGG.getId())) {
//                    return ENRICHMENT;
//                }
                // Everything else with a FoodAdditiveComponent
                return ADDITIVE;
            }
        }

        // 2) Flour
        if (stack.has(ModDataComponentTypes.FLOUR_TYPE.get())) {
            return FLOUR;
        }

        // 3) Tags for liquids, salts, yeasts, fats, sugars, additives, enrichments
        if (stack.is(IngredientTags.LIQUIDS))    return LIQUID;
        if (stack.is(IngredientTags.SALTS))      return SALT;
        if (stack.is(IngredientTags.YEASTS))     return YEAST;
        if (stack.is(IngredientTags.FATS))       return FAT;
        if (stack.is(IngredientTags.SUGARS))     return SUGAR;
        if (stack.is(IngredientTags.ADDITIVES))  return ADDITIVE;
        if (stack.is(IngredientTags.ENRICHMENTS))return ENRICHMENT;

        // 4) Fallback
        return CUSTOM;
    }

    // === CODEC and STREAM_CODEC as before ===
    public static final Codec<IngredientCategory> CODEC =
            Codec.STRING.xmap(IngredientCategory::valueOf, IngredientCategory::name);

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientCategory> STREAM_CODEC =
            new StreamCodec<RegistryFriendlyByteBuf, IngredientCategory>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buffer, IngredientCategory value) {
                    buffer.writeInt(value.ordinal());
                }
                @Override
                public IngredientCategory decode(RegistryFriendlyByteBuf buffer) {
                    int ord = buffer.readInt();
                    IngredientCategory[] vals = IngredientCategory.values();
                    return (ord >= 0 && ord < vals.length) ? vals[ord] : CUSTOM;
                }
            };

    public static StreamCodec<RegistryFriendlyByteBuf, List<IngredientCategory>> listOf() {
        return new StreamCodec<RegistryFriendlyByteBuf, List<IngredientCategory>>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buffer, List<IngredientCategory> list) {
                buffer.writeInt(list.size());
                for (var cat : list) {
                    STREAM_CODEC.encode(buffer, cat);
                }
            }
            @Override
            public List<IngredientCategory> decode(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readInt();
                List<IngredientCategory> out = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    out.add(STREAM_CODEC.decode(buffer));
                }
                return out;
            }
        };
    }
}
