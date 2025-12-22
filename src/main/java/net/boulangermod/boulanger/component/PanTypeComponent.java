package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record PanTypeComponent(String id) {

    private static final String MODID = "boulanger";

    // Canonicalize on construction so equals()/hashCode() are stable
    public PanTypeComponent {
        Objects.requireNonNull(id, "id");
        PanType t = PanType.byId(normalize(id));  // accept namespaced or bare
        if (t == null) {
            throw new IllegalArgumentException("Unknown PanType id: " + id);
        }
        id = t.getId(); // normalize to the enum's canonical (namespaced) id, e.g. "boulanger:baguette"
    }

    // Preferred factory
    public static PanTypeComponent of(PanType type) {
        Objects.requireNonNull(type, "PanType");
        return new PanTypeComponent(type.getId()); // reuses canonicalization above
    }

    // Accept arbitrary string and normalize
    public static PanTypeComponent ofId(String id) {
        return new PanTypeComponent(id); // reuses canonicalization above
    }

    /** Resolve enum from the stored id string. */
    public PanType toPanType() {
        return PanType.byId(normalize(id));
    }

    /** Convenience accessors for model indices. */
    public int getEmptyModelIndex() { return toPanType().getEmptyModelIndex(); }
    public int getFullModelIndex()  { return toPanType().getFullModelIndex(); }
    /** Back-compat helper: default to the EMPTY model. */
    public int getModelIndex()      { return getEmptyModelIndex(); }

    // ---- codecs ----
    public static final Codec<PanTypeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(PanTypeComponent::id)
            ).apply(instance, PanTypeComponent::new) // canonicalizes via compact ctor
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PanTypeComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, PanTypeComponent::id,
                    PanTypeComponent::new // canonicalizes via compact ctor
            );

    // ---- helpers ----
    private static ResourceLocation normalize(String s) {
        if (s == null || s.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        // If caller passed a bare id like "baguette", prefix our modid.
        return rl != null ? rl : ResourceLocation.fromNamespaceAndPath(MODID, s);
    }
}
