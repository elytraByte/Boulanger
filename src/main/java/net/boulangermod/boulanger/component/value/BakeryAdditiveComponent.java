package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BakeryAdditiveComponent(String id) {

    public static final Codec<BakeryAdditiveComponent> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.STRING.fieldOf("id").forGetter(BakeryAdditiveComponent::id)
            ).apply(inst, BakeryAdditiveComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BakeryAdditiveComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, BakeryAdditiveComponent::id,
                    BakeryAdditiveComponent::new
            );
}
