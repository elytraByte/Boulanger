package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;

public record ProofingStateComponent(
        int stepIndex,      // index in DoughProcessRecipe.steps
        int ticksInStep,    // time spent in current step
        boolean shaped
) {
    public static final Codec<ProofingStateComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("step_index").forGetter(ProofingStateComponent::stepIndex),
            Codec.INT.fieldOf("ticks_in_step").forGetter(ProofingStateComponent::ticksInStep),
            Codec.BOOL.fieldOf("shaped").forGetter(ProofingStateComponent::shaped)
    ).apply(instance, ProofingStateComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProofingStateComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.INT, ProofingStateComponent::stepIndex,
                    StreamCodecsCompat.INT, ProofingStateComponent::ticksInStep,
                    StreamCodecsCompat.BOOL, ProofingStateComponent::shaped,
                    ProofingStateComponent::new
            );

}

