package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ProcessingStep(
        StepType type,
        int minutes
) {
    public static final Codec<ProcessingStep> CODEC = RecordCodecBuilder.create(i -> i.group(
            StepType.CODEC.fieldOf("type").forGetter(ProcessingStep::type),
            Codec.INT.fieldOf("minutes").forGetter(ProcessingStep::minutes)
    ).apply(i, ProcessingStep::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingStep> STREAM_CODEC =
            StreamCodec.of(
                    (buf, v) -> {
                        StepType.STREAM_CODEC.encode(buf, v.type());
                        buf.writeVarInt(v.minutes());
                    },
                    buf -> {
                        StepType t = StepType.STREAM_CODEC.decode(buf);
                        int m = buf.readVarInt();
                        return new ProcessingStep(t, m);
                    }
            );

    public int durationTicks() { return Math.max(0, minutes) * 20 * 60; }

    public int getMinutes() { return minutes; } // optional
}
