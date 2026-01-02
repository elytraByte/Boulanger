package net.boulangermod.boulanger.content.bread;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum BreadType {
    BAGUETTE           (1,  "baguette"),
    MINI_BAGUETTE      (2,  "mini_baguette"),
    FRENCH_CROISSANT   (3,  "french_croissant"),
    CROISSANT          (4,  "croissant"),
    WHOLE_WHEAT_BREAD  (5,  "whole_wheat_bread"),
    BANH_MI            (6,  "banh_mi"),
    BRIOCHE            (7,  "brioche"),
    POTATO_BREAD       (8,  "potato_bread"),
    PAIN_DE_MI         (9,  "pain_de_mi"),
    WHITE_PAN_BREAD    (10, "white_pan_bread"),
    WHITE_BREAD        (11, "white_bread"),
    MULTIGRAIN_BREAD   (12, "multigrain_bread"),
    HAWAIIAN_ROLL      (13, "hawaiian_roll"),
    BRIOCHE_ROLL       (14, "brioche_roll"),
    MARBLE_RYE         (15, "marble_rye"),
    RYE_ROLL           (16, "rye_roll"),
    WHEAT_ROLL         (17, "wheat_roll"),
    SWEET_YEAST_ROLL   (18, "sweet_yeast_roll"),
    YEAST_ROLL         (19, "yeast_roll"),
    TEXAS_SWEET_ROLL   (20, "texas_roadhouse_roll"),
    SOURDOUGH          (21, "sourdough"),
    PRETZEL            (22, "pretzel"),
    BOULE              (23, "boule");

    private final int modelIndex;
    private final String id;

    BreadType(int modelIndex, String id) {
        this.modelIndex = modelIndex;
        this.id = id;
    }

    public String id() { return id; }
    public int modelIndex() { return modelIndex; }

    public ResourceLocation rl() {
        return ResourceLocation.fromNamespaceAndPath("boulanger", id);
    }

    private static final Map<String, BreadType> BY_ID =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(BreadType::id, t -> t));

    public static Optional<BreadType> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<BreadType> fromRecipeId(ResourceLocation recipeId) {
        return (recipeId == null) ? Optional.empty() : byId(recipeId.getPath());
    }

    public static final Codec<BreadType> CODEC = Codec.STRING.flatXmap(
            idStr -> byId(idStr)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() ->
                            "Unknown bread type: " + idStr +
                                    " (valid: " + String.join(", ", BY_ID.keySet()) + ")")),
            bt -> DataResult.success(bt.id())
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BreadType> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING,
                    BreadType::id,
                    idStr -> byId(idStr).orElse(BAGUETTE)
            );
}
