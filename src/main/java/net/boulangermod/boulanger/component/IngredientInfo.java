// src/main/java/net/boulangermod/boulanger/component/IngredientInfo.java
package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Per-ingredient snapshot used in DoughRecipeComponent.
 * - Milligrams are the source of truth.
 * - Codecs are backward compatible with legacy "grams" snapshots.
 */
public record IngredientInfo(
        String itemId,                 // registry id of the *item* (e.g. boulanger:flour, boulanger:butter, minecraft:water_bucket)
        IngredientCategory category,   // FLOUR, WATER, ADDITIVE, etc.
        int milligrams,                // precise weight (mg)
        @Nullable FlourType flourType  // optional flour variant (only for FLOUR category)
) {
    // ---- Constructors / helpers --------------------------------------------------

    /**
     * Legacy-style constructor that accepts grams and converts to mg.
     */
    public static IngredientInfo of(String itemId, IngredientCategory category, int grams) {
        return new IngredientInfo(itemId, category, Math.max(0, grams) * 1000, null);
    }

    /**
     * New precise constructor for mg.
     */
    public static IngredientInfo ofMg(String itemId, IngredientCategory category, int milligrams) {
        return new IngredientInfo(itemId, category, Math.max(0, milligrams), null);
    }

    /**
     * Attach a flour type (used only when category == FLOUR).
     */
    public IngredientInfo withFlourType(FlourType ft) {
        return new IngredientInfo(this.itemId, this.category, this.milligrams, ft);
    }

    // ---- CODEC (NBT/JSON) with back-compat: prefer "milligrams", fall back to "grams" ----

    public static final Codec<IngredientInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("itemId").forGetter(IngredientInfo::itemId),
            IngredientCategory.CODEC.fieldOf("category").forGetter(IngredientInfo::category),

            // Prefer milligrams if present; otherwise accept legacy grams
            Codec.INT.optionalFieldOf("milligrams").forGetter(i -> Optional.of(i.milligrams)),
            Codec.INT.optionalFieldOf("grams").forGetter(i -> Optional.empty()),

            FlourType.CODEC.optionalFieldOf("flourType").forGetter(i -> Optional.ofNullable(i.flourType))
    ).apply(inst, (itemId, category, mgOpt, gramsOpt, flourOpt) -> {
        int mg = mgOpt.orElseGet(() -> gramsOpt.map(g -> Math.max(0, g) * 1000).orElse(0));
        return new IngredientInfo(itemId, category, mg, flourOpt.orElse(null));
    }));

    // ---- STREAM_CODEC (network) --------------------------------------------------
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientInfo> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,                 // item id
                    IngredientInfo::itemId,

                    IngredientCategory.STREAM_CODEC,           // category (ensure this is also RegistryFriendlyByteBuf-typed)
                    IngredientInfo::category,

                    ByteBufCodecs.VAR_INT,                     // milligrams
                    IngredientInfo::milligrams,

                    // Flour type encoded as Optional<String> id (no need for a special nullable codec)
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
                    i -> java.util.Optional.ofNullable(i.flourType()).map(FlourType::getId),

                    (itemId, category, mg, flourIdOpt) -> new IngredientInfo(
                            itemId,
                            category,
                            mg,
                            flourIdOpt
                                    .map(id -> net.boulangermod.boulanger.item.FlourItemType.fromId(id).toFlourType())
                                    .orElse(null)
                    )
            );
}
