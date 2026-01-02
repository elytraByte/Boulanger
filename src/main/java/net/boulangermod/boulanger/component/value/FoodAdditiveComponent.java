package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record FoodAdditiveComponent(String id) {

    public static final Codec<FoodAdditiveComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(FoodAdditiveComponent::id)
            ).apply(instance, FoodAdditiveComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FoodAdditiveComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, FoodAdditiveComponent::id,
                    FoodAdditiveComponent::new
            );
}
