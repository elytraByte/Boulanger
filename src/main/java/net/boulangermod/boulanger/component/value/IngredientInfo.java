package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Per-ingredient snapshot used inside DoughRecipeComponent.
 * Source of truth: integer milligrams.
 *
 * JSON/NBT:
 * - Writes "milligrams"
 * - Accepts legacy "grams" for backward compatibility (converted to mg)
 */
public record IngredientInfo(
        String itemId,                 // registry id of the ingredient item (namespaced, e.g. "boulanger:flour")
        IngredientCategory category,    // FLOUR, WATER, ADDITIVE, etc.
        int milligrams,                // weight in mg (int)
        @Nullable FlourType flourType   // only used for FLOUR category
) {

    // --- Factories ---------------------------------------------------------------

    /** Legacy helper: grams -> mg. */
    public static IngredientInfo ofGrams(String itemId, IngredientCategory category, int grams) {
        return new IngredientInfo(itemId, category, Math.max(0, grams) * 1000, null);
    }

    /** Preferred helper: mg. */
    public static IngredientInfo ofMg(String itemId, IngredientCategory category, int milligrams) {
        return new IngredientInfo(itemId, category, Math.max(0, milligrams), null);
    }

    public IngredientInfo withFlourType(@Nullable FlourType ft) {
        return new IngredientInfo(this.itemId, this.category, this.milligrams, ft);
    }

    // --- CODEC (NBT/JSON) -------------------------------------------------------

    public static final Codec<IngredientInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("itemId").forGetter(IngredientInfo::itemId),
            IngredientCategory.CODEC.fieldOf("category").forGetter(IngredientInfo::category),

            // Write mg; accept legacy grams if present
            Codec.INT.optionalFieldOf("milligrams").forGetter(i -> Optional.of(i.milligrams())),
            Codec.INT.optionalFieldOf("grams").forGetter(i -> Optional.empty()),

            FlourType.CODEC.optionalFieldOf("flourType").forGetter(i -> Optional.ofNullable(i.flourType()))
    ).apply(inst, (itemId, category, mgOpt, gramsOpt, flourOpt) -> {
        int mg = mgOpt.orElseGet(() -> gramsOpt.map(g -> Math.max(0, g) * 1000).orElse(0));
        return new IngredientInfo(itemId, category, Math.max(0, mg), flourOpt.orElse(null));
    }));

    // --- STREAM_CODEC (network) -------------------------------------------------

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientInfo> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, IngredientInfo v) {
                    StreamCodecsCompat.STRING.encode(buf, v.itemId());
                    IngredientCategory.STREAM_CODEC.encode(buf, v.category());
                    StreamCodecsCompat.VAR_INT.encode(buf, v.milligrams());

                    // Optional flour type (encode directly; no enum lookup/coupling)
                    buf.writeBoolean(v.flourType() != null);
                    if (v.flourType() != null) {
                        FlourType.STREAM_CODEC.encode(buf, v.flourType());
                    }
                }

                @Override
                public IngredientInfo decode(RegistryFriendlyByteBuf buf) {
                    String itemId = StreamCodecsCompat.STRING.decode(buf);
                    IngredientCategory cat = IngredientCategory.STREAM_CODEC.decode(buf);
                    int mg = StreamCodecsCompat.VAR_INT.decode(buf);

                    FlourType ft = buf.readBoolean() ? FlourType.STREAM_CODEC.decode(buf) : null;
                    return new IngredientInfo(itemId, cat, Math.max(0, mg), ft);
                }
            };
}
