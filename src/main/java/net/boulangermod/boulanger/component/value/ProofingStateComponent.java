package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record ProofingStateComponent(
        int stepIndex,     // 0-based index into DoughProcessRecipe.steps
        int ticksInStep,   // ticks spent in current step
        boolean shaped     // has been shaped (affects allowed steps / baking)
) {
    public ProofingStateComponent {
        stepIndex = Math.max(0, stepIndex);
        ticksInStep = Math.max(0, ticksInStep);
    }

    public static final Codec<ProofingStateComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("stepIndex").forGetter(ProofingStateComponent::stepIndex),
            Codec.INT.fieldOf("ticksInStep").forGetter(ProofingStateComponent::ticksInStep),
            Codec.BOOL.fieldOf("shaped").forGetter(ProofingStateComponent::shaped)
    ).apply(inst, ProofingStateComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProofingStateComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.VAR_INT, ProofingStateComponent::stepIndex,
                    StreamCodecsCompat.VAR_INT, ProofingStateComponent::ticksInStep,
                    StreamCodecsCompat.BOOLEAN, ProofingStateComponent::shaped,
                    ProofingStateComponent::new
            );

    public static ProofingStateComponent start() {
        return new ProofingStateComponent(0, 0, false);
    }

    public ProofingStateComponent withStepIndex(int stepIndex) {
        return new ProofingStateComponent(stepIndex, this.ticksInStep, this.shaped);
    }

    public ProofingStateComponent withTicksInStep(int ticksInStep) {
        return new ProofingStateComponent(this.stepIndex, ticksInStep, this.shaped);
    }

    public ProofingStateComponent withShaped(boolean shaped) {
        return new ProofingStateComponent(this.stepIndex, this.ticksInStep, shaped);
    }
}
