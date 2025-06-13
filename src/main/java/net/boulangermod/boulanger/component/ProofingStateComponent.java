package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;

public record ProofingStateComponent(boolean proofed, int punchCount) {

    // JSON/NBT Codec
    public static final Codec<ProofingStateComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("proofed").forGetter(ProofingStateComponent::proofed),
                    Codec.INT.fieldOf("punchCount").forGetter(ProofingStateComponent::punchCount)
            ).apply(instance, ProofingStateComponent::new)
    );

    // Network StreamCodec
    public static final StreamCodec<RegistryFriendlyByteBuf, ProofingStateComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ProofingStateComponent::proofed,
                    ByteBufCodecs.VAR_INT,
                    ProofingStateComponent::punchCount,
                    ProofingStateComponent::new
            );
}
