package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

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

    // ─────────────────────────────────────────────────────────────────────
    // Convenience constructors for common dev/testing states
    // ─────────────────────────────────────────────────────────────────────

    /** Start state: before any proofing. */
    public static ProofingStateComponent mixed() {
        return new ProofingStateComponent(0, 0, false);
    }

    /**
     * After the first bulk proof has completed (i.e., ready for the first punchdown).
     * Uses stepIndex=1 as a sensible default for pipelines that start with PROOF at index 0.
     * Prefer {@link #bulkProofedAt(int)} when you know the actual index.
     */
    public static ProofingStateComponent bulkProofed() {
        return new ProofingStateComponent(1, 0, false);
    }

    /**
     * Final-proofed fallback (recipe-agnostic).
     * Marks as shaped and assumes the final proof is “complete” from a UI/oven perspective.
     * Prefer {@link #finalProofedAt(int)} when you know the real final-proof step index.
     */
    public static ProofingStateComponent finalProofed() {
        // Use index 1 as a safe non-crashing default and a large tick count so “complete” checks pass.
        // (If your oven/proofer requires the exact final step index, call finalProofedAt(...) instead.)
        return new ProofingStateComponent(1, Integer.MAX_VALUE / 4, true);
    }

    /** Recipe-aware variant: bulk-proof completed; next step is at {@code nextStepIndex}. */
    public static ProofingStateComponent bulkProofedAt(int nextStepIndex) {
        return new ProofingStateComponent(Math.max(0, nextStepIndex), 0, false);
    }

    /**
     * Recipe-aware variant: already in the final proof step, with a large tick count so it’s “done”.
     * Pass the index of the final PROOF step from your DoughProcessRecipe.
     */
    public static ProofingStateComponent finalProofedAt(int finalProofStepIndex) {
        return new ProofingStateComponent(Math.max(0, finalProofStepIndex), Integer.MAX_VALUE / 4, true);
    }
}
