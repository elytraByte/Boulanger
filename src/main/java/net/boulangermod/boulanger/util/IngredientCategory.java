package net.boulangermod.boulanger.util;

import com.mojang.serialization.Codec;
import net.boulangermod.boulanger.component.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.FoodAdditiveType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public enum IngredientCategory {
    FLOUR,
    DAIRY,
    WATER,
    EGGS,
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

        // 0) If something (e.g. the Scale) already set an explicit category on this stack, return it:
        if (stack.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())) {
            IngredientCategory cat = stack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
            if (cat != null) {
                return cat;
            }
        }

        // 1) If it has a FoodAdditiveComponent, defer to the enum’s built-in category:
        if (stack.has(ModDataComponentTypes.FOOD_ADDITIVE.get())) {
            FoodAdditiveComponent comp = stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
            if (comp != null) {
                return FoodAdditiveType
                        .fromId(comp.getId())
                        .getCategory();
            }
        }

        // 2) Flour items:
        if (stack.has(ModDataComponentTypes.FLOUR_TYPE.get())) {
            return FLOUR;
        }

        // 3) (Optional) Raw-item tags, if you’ve actually tagged your vanilla/ModItems:
        if (stack.is(IngredientTags.WATER))     return WATER;
        if (stack.is(IngredientTags.EGGS))      return EGGS;
        if (stack.is(IngredientTags.DAIRY))     return DAIRY;
        if (stack.is(IngredientTags.SALTS))     return SALT;
        if (stack.is(IngredientTags.YEASTS))    return YEAST;
        if (stack.is(IngredientTags.FATS))      return FAT;
        if (stack.is(IngredientTags.SUGARS))    return SUGAR;
        if (stack.is(IngredientTags.ADDITIVES)) return ADDITIVE;
        if (stack.is(IngredientTags.ENRICHMENTS)) return ENRICHMENT;

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
