package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record PanTypeComponent(String id) {

    // Canonicalize on construction so equals() is stable
    public PanTypeComponent {
        Objects.requireNonNull(id, "id");
        PanType t = PanType.fromId(id);         // accept namespaced or bare
        if (t == null) throw new IllegalArgumentException("Unknown PanType id: " + id);
        id = t.getId();                         // <<< normalize to canonical enum id (e.g., "baguette")
    }

    // Preferred factory
    public static PanTypeComponent of(PanType type) {
        Objects.requireNonNull(type, "PanType");
        return new PanTypeComponent(type.getId()); // goes through canonicalization above
    }

    // Accept arbitrary string and normalize
    public static PanTypeComponent ofId(String id) {
        return new PanTypeComponent(id);           // goes through canonicalization above
    }

    public PanType toPanType() { return PanType.fromId(id); }
    public int getModelIndex() { return toPanType().getModelIndex(); }

    public static final Codec<PanTypeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(PanTypeComponent::id)
            ).apply(instance, PanTypeComponent::new)     // canonicalizes via compact ctor
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PanTypeComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, PanTypeComponent::id,
                    PanTypeComponent::new                           // canonicalizes via compact ctor
            );
}