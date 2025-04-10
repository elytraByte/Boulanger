package net.boulangermod.boulanger.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.boulangermod.boulanger.util.StreamCodecsCompat;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum WheatVariety {
    HARD_RED_WINTER("hard_red_winter"),
    HARD_RED_SPRING("hard_red_spring"),
    DURUM("durum"),
    SOFT_RED_WINTER("soft_red_winter"),
    HARD_WHITE_SPRING("hard_white_spring");

    private final String id;
    private static final Map<String, WheatVariety> BY_ID =
            Arrays.stream(values()).collect(Collectors.toMap(WheatVariety::getId, v -> v));

    WheatVariety(String id) { this.id = id; }
    public String getId() { return id; }
    public static WheatVariety fromId(String id) {
        return BY_ID.getOrDefault(id, HARD_RED_WINTER);
    }

    /** For JSON/NBT persistence */
    public static final Codec<WheatVariety> CODEC = Codec.STRING.xmap(
            WheatVariety::fromId,
            WheatVariety::getId
    );

    /** For network synchronization */
    public static final StreamCodec<RegistryFriendlyByteBuf, WheatVariety> STREAM_CODEC =
            StreamCodecsCompat.STRING.map(
                    WheatVariety::fromId,
                    WheatVariety::getId
            );
}
