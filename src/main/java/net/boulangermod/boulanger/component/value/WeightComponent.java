package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WeightComponent(int milligrams) {

    public static WeightComponent ofMilligrams(int mg) {
        return new WeightComponent(Math.max(0, mg));
    }

    public static WeightComponent ofGrams(int grams) {
        return ofMilligrams(Math.max(0, grams) * 1000);
    }

    public double grams() {
        return milligrams / 1000.0;
    }

    // --- Codecs ---
    public static final Codec<WeightComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("mg").forGetter(WeightComponent::milligrams)
            ).apply(instance, WeightComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WeightComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.VAR_INT, WeightComponent::milligrams,
                    WeightComponent::new
            );
}
