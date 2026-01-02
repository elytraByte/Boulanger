package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.content.pan.PanType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record PanServing(int servingWeightG, PanType panType, int perPanCapacity) {

    public PanServing {
        if (servingWeightG <= 0) {
            throw new IllegalArgumentException("servingWeightG must be > 0 (got " + servingWeightG + ")");
        }
        Objects.requireNonNull(panType, "panType");
        if (perPanCapacity <= 0) {
            throw new IllegalArgumentException("perPanCapacity must be > 0 (got " + perPanCapacity + ")");
        }
    }

    public static final MapCodec<PanServing> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecInts.POSITIVE.fieldOf("serving_weight_g").forGetter(PanServing::servingWeightG),
            PanType.CODEC.fieldOf("pan_type").forGetter(PanServing::panType),
            CodecInts.POSITIVE.fieldOf("per_pan_capacity").forGetter(PanServing::perPanCapacity)
    ).apply(instance, PanServing::new));

    public static final Codec<PanServing> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<ByteBuf, PanServing> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PanServing::servingWeightG,
                    PanType.STREAM_CODEC,  PanServing::panType,       // <-- stable (not ordinal)
                    ByteBufCodecs.VAR_INT, PanServing::perPanCapacity,
                    PanServing::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, PanServing> STREAM_CODEC_RF =
            STREAM_CODEC.cast();
}
