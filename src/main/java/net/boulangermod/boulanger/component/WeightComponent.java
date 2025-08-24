package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WeightComponent(float grams) {

    // ── Factories ─────────────────────────────────────────────────────────────
    /** Preferred factory: grams as float. */
    public static WeightComponent ofGrams(float grams) { return new WeightComponent(grams); }

    /** Convenience overload for double. */
    public static WeightComponent ofGrams(double grams) { return new WeightComponent((float) grams); }

    /** Create from milligrams (rounded by caller if needed). */
    public static WeightComponent ofMilligrams(long mg) { return new WeightComponent(mg / 1000f); }

    /** Zero weight. */
    public static WeightComponent zero() { return new WeightComponent(0f); }

    // ── Codecs ────────────────────────────────────────────────────────────────
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    /** Alias; keep for older call sites. */
    public float getWeight() { return grams; }

    /** Milligrams, rounded to nearest whole mg. */
    public long milligrams() { return Math.round(this.grams * 1000.0); }

    /** Non-negative copy (useful if subtracting). */
    public WeightComponent clampNonNegative() { return grams < 0f ? zero() : this; }
}
