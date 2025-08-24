package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;
import java.util.Objects;

public record BakerPctComponent(Map<IngredientCategory, Double> percentages) {

    // Compact ctor: reassign the *parameter*; don't touch this.percentages
    public BakerPctComponent {
        Objects.requireNonNull(percentages, "percentages");
        percentages = Map.copyOf(percentages);
    }

    // Convenience factory
    public static BakerPctComponent of(Map<IngredientCategory, Double> map) {
        return new BakerPctComponent(map);
    }

    // --- network ---
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

    // --- json/nbt ---
    private static final Codec<Map<IngredientCategory, Double>> CODEC_OF_MAP =
            StreamCodecsCompat.mapCodec(IngredientCategory.CODEC, Codec.DOUBLE);

    public static final Codec<BakerPctComponent> CODEC =
            CODEC_OF_MAP.xmap(BakerPctComponent::new, BakerPctComponent::percentages);
}
