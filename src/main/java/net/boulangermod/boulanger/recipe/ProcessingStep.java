package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A single step in a dough process pipeline.
 * Minutes-native (converted to ticks via durationTicks()).
 */
public record ProcessingStep(
        StepType type,
        int minutes
) {
    public static final int MAX_MINUTES = 60 * 24 * 7;

    public ProcessingStep {
        if (type == null) throw new NullPointerException("type");
        minutes = Math.max(0, minutes);
    }

    public static final Codec<ProcessingStep> CODEC = RecordCodecBuilder.create(i -> i.group(
            StepType.CODEC.fieldOf("type").forGetter(ProcessingStep::type),
            Codec.intRange(0, MAX_MINUTES).fieldOf("minutes").forGetter(ProcessingStep::minutes)
    ).apply(i, ProcessingStep::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingStep> STREAM_CODEC =
            StreamCodec.of(
                    (buf, v) -> {
                        StepType.STREAM_CODEC.encode(buf, v.type());
                        buf.writeVarInt(v.minutes());
                    },
                    buf -> new ProcessingStep(
                            StepType.STREAM_CODEC.decode(buf),
                            buf.readVarInt()
                    )
            );

    /**
     * @return duration in game ticks (20 ticks/sec)
     */
    public int durationTicks() {
        // 20 ticks/sec * 60 sec/min
        return minutes * 20 * 60;
    }
}
