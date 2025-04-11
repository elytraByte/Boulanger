package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WeightComponent(float grams) {
    public static final Codec<WeightComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("grams").forGetter(WeightComponent::grams)
            ).apply(instance, WeightComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WeightComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.FLOAT, WeightComponent::grams,
                    WeightComponent::new
            );

    public float getWeight() {

        return grams;
    }
}

