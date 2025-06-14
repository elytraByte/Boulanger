package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ProcessingStep(
        StepType type,
        int durationTicks // optional for some step types
) {
    public static final Codec<ProcessingStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StepType.CODEC.fieldOf("type").forGetter(ProcessingStep::type),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(ProcessingStep::durationTicks)
    ).apply(instance, ProcessingStep::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingStep> STREAM_CODEC =
            StreamCodec.composite(
                    StepType.STREAM_CODEC, ProcessingStep::type,
                    StreamCodecsCompat.INT, ProcessingStep::durationTicks,
                    ProcessingStep::new
            );

}
