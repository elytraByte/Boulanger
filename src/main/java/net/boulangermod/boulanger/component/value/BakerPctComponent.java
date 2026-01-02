package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record BakerPctComponent(Map<IngredientCategory, Double> percentages) {

    public BakerPctComponent {
        Objects.requireNonNull(percentages, "percentages");

        // Ensure enum map + stable iteration order
        EnumMap<IngredientCategory, Double> tmp = new EnumMap<>(IngredientCategory.class);
        tmp.putAll(percentages);
        percentages = Map.copyOf(tmp);
    }

    public static BakerPctComponent of(Map<IngredientCategory, Double> map) {
        return new BakerPctComponent(map);
    }

    // --- JSON/NBT ---
    // clean and readable: { "FLOUR": 100.0, "WATER": 65.0, ... }
    private static final Codec<Map<IngredientCategory, Double>> MAP_CODEC =
            Codec.unboundedMap(IngredientCategory.CODEC, Codec.DOUBLE);

    public static final Codec<BakerPctComponent> CODEC =
            MAP_CODEC.xmap(BakerPctComponent::new, BakerPctComponent::percentages);

    // --- Network (RegistryFriendlyByteBuf) ---
    public static final StreamCodec<RegistryFriendlyByteBuf, BakerPctComponent> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, BakerPctComponent comp) {
                    StreamCodecsCompat.VAR_INT.encode(buf, comp.percentages().size());
                    for (var e : comp.percentages().entrySet()) {
                        IngredientCategory.STREAM_CODEC.encode(buf, e.getKey());
                        ByteBufCodecs.DOUBLE.encode(buf, e.getValue());
                    }
                }

                @Override
                public BakerPctComponent decode(RegistryFriendlyByteBuf buf) {
                    int size = StreamCodecsCompat.VAR_INT.decode(buf);
                    EnumMap<IngredientCategory, Double> map = new EnumMap<>(IngredientCategory.class);
                    for (int i = 0; i < size; i++) {
                        IngredientCategory cat = IngredientCategory.STREAM_CODEC.decode(buf);
                        double pct = ByteBufCodecs.DOUBLE.decode(buf);
                        map.put(cat, pct);
                    }
                    return new BakerPctComponent(map);
                }
            };
}
