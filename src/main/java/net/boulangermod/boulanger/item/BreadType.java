package net.boulangermod.boulanger.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Optional;

public enum BreadType {
    BAGUETTE (1, "baguette"),
    CROISSANT(2, "croissant"),
    WHOLE_WHEAT_BREAD(3, "whole_wheat_bread");

    private final int modelIndex;
    private final String id;

    BreadType(int modelIndex, String id) {
        this.modelIndex = modelIndex;
        this.id = id;
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public String getId() {
        return id;
    }

    public String id() {
        return id;
    }

    public static Optional<BreadType> byId(String id) {
        return Arrays.stream(values())
                .filter(bt -> bt.getId().equals(id))
                .findFirst();
    }


    // === JSON / data‐driven codec ===
    public static final Codec<BreadType> CODEC = Codec.STRING.flatXmap(
            // from string -> enum
            idStr -> {
                for (var bt : values()) {
                    if (bt.id().equals(idStr)) {
                        return DataResult.success(bt);
                    }
                }
                // wrap your error message in a Supplier<String>:
                return DataResult.error(() -> "Unknown bread type: " + idStr);
            },
            // from enum -> string
            bt -> DataResult.success(bt.id())
    );

    // === network sync codec ===
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, BreadType> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    net.boulangermod.boulanger.util.StreamCodecsCompat.STRING,
                    BreadType::getId,
                    idStr -> {
                        for (var bt : values()) {
                            if (bt.id().equals(idStr)) {
                                return bt;
                            }
                        }
                        return BAGUETTE; // fallback
                    }
            );

    public ResourceLocation rl() {
        return ResourceLocation.fromNamespaceAndPath("boulanger", id);
    }

}
