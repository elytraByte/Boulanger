package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.item.PanType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public record PanServing(int servingWeightG, PanType panType, int perPanCapacity) {
    public static final MapCodec<PanServing> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecInts.POSITIVE.fieldOf("serving_weight_g").forGetter(PanServing::servingWeightG),
            PanType.CODEC.fieldOf("pan_type").forGetter(PanServing::panType),
            CodecInts.POSITIVE.fieldOf("per_pan_capacity").forGetter(PanServing::perPanCapacity)
    ).apply(instance, PanServing::new));

    public static final StreamCodec<ByteBuf, PanServing> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PanServing::servingWeightG,
                    ByteBufCodecs.idMapper(
                            (IntFunction<PanType>) i -> PanType.values()[i],
                            (ToIntFunction<PanType>) PanType::ordinal
                    ),
                    PanServing::panType,
                    ByteBufCodecs.VAR_INT, PanServing::perPanCapacity,
                    PanServing::new
            );

    public static final Codec<PanServing> VALUE_CODEC = CODEC.codec();
}
