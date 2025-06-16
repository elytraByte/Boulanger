package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record PanTypeComponent(String id) {

    public static final Codec<PanTypeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(PanTypeComponent::id)
            ).apply(instance, PanTypeComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PanTypeComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, PanTypeComponent::id,
                    PanTypeComponent::new
            );

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PanTypeComponent panType)) return false;
        return Objects.equals(id, panType.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }

    public int getModelIndex() {
        return PanType.fromId(id).getModelIndex();
    }
}
