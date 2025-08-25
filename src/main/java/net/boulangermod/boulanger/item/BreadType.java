package net.boulangermod.boulanger.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum BreadType {
    // keep existing mappings stable
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

    // new additions
    BRIOCHE_ROLL       (14, "brioche_roll"),
    MARBLE_RYE         (15, "marble_rye"),
    RYE_ROLL           (16, "rye_roll"),
    WHEAT_ROLL         (17, "wheat_roll"),
    SWEET_YEAST_ROLL   (18, "sweet_yeast_roll"),
    YEAST_ROLL         (19, "yeast_roll");

    private final int modelIndex;
    private final String id;

    BreadType(int modelIndex, String id) {
        this.modelIndex = modelIndex;
        this.id = id;
    }

    public static Optional<BreadType> fromRecipeId(ResourceLocation recipeId) {
        return byId(recipeId.getPath());
    }

    public int getModelIndex() { return modelIndex; }
    public String getId()      { return id; }
    public String id()         { return id; }

    public static Optional<BreadType> byId(String id) {
        Optional<BreadType> direct = Arrays.stream(values())
                .filter(bt -> bt.getId().equals(id))
                .findFirst();
        if (direct.isPresent()) return direct;

        return Optional.empty();
    }

    // === JSON / data-driven codec ===
    public static final Codec<BreadType> CODEC = Codec.STRING.flatXmap(
            idStr -> byId(idStr)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(
                            () -> "Unknown bread type: " + idStr +
                                    " (valid: " + Arrays.stream(values()).map(BreadType::id).collect(Collectors.joining(", ")) + ")"
                    )),
            bt -> DataResult.success(bt.id())
    );

    // === network sync codec ===
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, BreadType> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    net.boulangermod.boulanger.util.StreamCodecsCompat.STRING,
                    BreadType::getId,
                    idStr -> byId(idStr).orElse(BAGUETTE) // fallback
            );

    public ResourceLocation rl() {
        return ResourceLocation.fromNamespaceAndPath("boulanger", id);
    }
}
