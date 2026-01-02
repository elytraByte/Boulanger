package net.boulangermod.boulanger.content.proofing;

import net.boulangermod.boulanger.component.value.ProofingStateComponent;

public final class ProofingStates {
    private ProofingStates() {}

    public static ProofingStateComponent mixed() {
        return ProofingStateComponent.start();
    }

    public static ProofingStateComponent bulkProofedAt(int nextStepIndex) {
        return new ProofingStateComponent(nextStepIndex, 0, false);
    }

    public static ProofingStateComponent finalProofedAt(int finalProofStepIndex) {
        return new ProofingStateComponent(finalProofStepIndex, Integer.MAX_VALUE / 4, true);
    }
}
