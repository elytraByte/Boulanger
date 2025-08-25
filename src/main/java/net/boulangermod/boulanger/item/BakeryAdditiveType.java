// src/main/java/net/boulangermod/boulanger/item/BakeryAdditiveType.java
package net.boulangermod.boulanger.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.boulangermod.boulanger.component.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum BakeryAdditiveType {
    ASCORBIC_ACID            ("ascorbic_acid",             454f, IngredientCategory.ADDITIVE),
    CALCIUM_PROPIONATE       ("calcium_propionate",        454f, IngredientCategory.ADDITIVE),
    DIASTATIC_MALT_POWDER    ("diastatic_malt_powder",     454f, IngredientCategory.ADDITIVE),
    NONDIASTATIC_MALT_POWDER ("nondiastatic_malt_powder",  454f, IngredientCategory.ADDITIVE),
    L_CYSTEINE               ("l_cysteine",                113f, IngredientCategory.ADDITIVE);

    private final String id;
    private final float weight; // default/reference package size in grams
    private final IngredientCategory category;

    BakeryAdditiveType(String id, float weight, IngredientCategory category) {
        this.id = id;
        this.weight = weight;
        this.category = category;
    }

    public String getId() { return id; }
    public float getWeight() { return weight; }
    public IngredientCategory getCategory() { return category; }
    public String id() { return id; }

    public ResourceLocation rl() {
        return ResourceLocation.fromNamespaceAndPath("boulanger", id);
    }

    // ── Lookup ──────────────────────────────────────────────────────────────────
    private static final Map<String, BakeryAdditiveType> BY_ID =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(BakeryAdditiveType::getId, e -> e));

    public static Optional<BakeryAdditiveType> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public FoodAdditiveComponent toFoodAdditiveComponent() {
        return new FoodAdditiveComponent(id, new WeightComponent(weight));
    }
    // If you prefer your original name:
    public FoodAdditiveComponent toBakeryAdditiveComponent() {
        return toFoodAdditiveComponent();
    }

    // ── JSON / persistence codec ────────────────────────────────────────────────
    public static final Codec<BakeryAdditiveType> CODEC = Codec.STRING.flatXmap(
            idStr -> byId(idStr)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(
                            () -> "Unknown bakery additive: " + idStr +
                                    " (valid: " + String.join(", ", BY_ID.keySet()) + ")")),
            type -> DataResult.success(type.id())
    );

    // ── Network codec ──────────────────────────────────────────────────────────
    public static final StreamCodec<RegistryFriendlyByteBuf, BakeryAdditiveType> STREAM_CODEC =
            StreamCodec.composite(
                    // Uses your helper from Model Set Context #33
                    net.boulangermod.boulanger.util.StreamCodecsCompat.STRING,
                    BakeryAdditiveType::getId,
                    idStr -> byId(idStr).orElse(ASCORBIC_ACID) // safe fallback
            );
}
