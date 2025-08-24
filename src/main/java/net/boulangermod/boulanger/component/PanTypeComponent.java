package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record PanTypeComponent(String id) {

    // ── Factories ─────────────────────────────────────────────────────────────
    /** Create from enum (preferred). */
    public static PanTypeComponent of(PanType type) {
        Objects.requireNonNull(type, "PanType");
        return new PanTypeComponent(type.getId());
    }

    /** Create from id string; validates/normalizes via PanType.fromId(id). */
    public static PanTypeComponent ofId(String id) {
        Objects.requireNonNull(id, "id");
        PanType t = PanType.fromId(id);
        if (t == null) throw new IllegalArgumentException("Unknown PanType id: " + id);
        return new PanTypeComponent(t.getId()); // normalize to canonical id
    }

    /** Convenience to get the enum back. */
    public PanType toPanType() {
        return PanType.fromId(id);
    }

    public int getModelIndex() {
        return toPanType().getModelIndex();
    }

    // ── Codecs ────────────────────────────────────────────────────────────────
    public static final Codec<PanTypeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(PanTypeComponent::id)
            ).apply(instance, PanTypeComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PanTypeComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, PanTypeComponent::id,
                    PanTypeComponent::new
            );
}
