package net.boulangermod.boulanger.content.ingredient;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

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
