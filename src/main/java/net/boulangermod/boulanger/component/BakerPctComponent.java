package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;

import java.util.Map;
import java.util.stream.Collectors;

public record BakerPctComponent(Map<IngredientCategory, Double> percentages) {
    public BakerPctComponent {
        // wrap in an unmodifiable copy to ensure immutability
        percentages = Map.copyOf(percentages);
    }

    // === network serialization ===
    public static final StreamCodec<FriendlyByteBuf, BakerPctComponent> STREAM_CODEC = StreamCodec.of(
            (buf, comp) -> {
                buf.writeInt(comp.percentages.size());
                comp.percentages.forEach((cat, pct) -> {
                    buf.writeEnum(cat);
                    buf.writeDouble(pct);
                });
            },
            buf -> {
                int size = buf.readInt();
                var map = new java.util.EnumMap<IngredientCategory, Double>(IngredientCategory.class);
                for (int i = 0; i < size; i++) {
                    IngredientCategory cat = buf.readEnum(IngredientCategory.class);
                    double pct = buf.readDouble();
                    map.put(cat, pct);
                }
                return new BakerPctComponent(map);
            }
    );

    // === JSON/NBT serialization ===
    private static final Codec<Map<IngredientCategory, Double>> CODEC_OF_MAP =
            StreamCodecsCompat.mapCodec(IngredientCategory.CODEC, Codec.DOUBLE);

    public static final Codec<BakerPctComponent> CODEC =
            CODEC_OF_MAP.xmap(BakerPctComponent::new, BakerPctComponent::percentages);
}
