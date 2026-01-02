package net.boulangermod.boulanger.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PortionKind implements StringRepresentable {
    LOAF("loaf", 0),
    ROLL("roll", 1),
    BAGUETTE("baguette", 2);

    private final String id;      // stable for JSON / commands / debugging
    private final int networkId;  // stable for networking

    PortionKind(String id, int networkId) {
        this.id = id;
        this.networkId = networkId;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public int networkId() {
        return networkId;
    }

    private static final Map<String, PortionKind> BY_ID =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                    k -> k.id,
                    Function.identity()
            ));

    public static PortionKind byId(String s) {
        if (s == null) return null;
        String key = s.toLowerCase(Locale.ROOT);

        PortionKind byId = BY_ID.get(key);
        if (byId != null) return byId;

        try {
            return PortionKind.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static final Codec<PortionKind> CODEC = Codec.STRING.comapFlatMap(s -> {
        PortionKind k = byId(s);
        return k != null
                ? DataResult.success(k)
                : DataResult.error(() -> "Unknown PortionKind: " + s);
    }, PortionKind::getSerializedName);

    public static final StreamCodec<ByteBuf, PortionKind> STREAM_CODEC =
            ByteBufCodecs.idMapper(
                    id -> {
                        for (PortionKind k : values()) {
                            if (k.networkId == id) return k;
                        }
                        return LOAF;
                    },
                    PortionKind::networkId
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, PortionKind> STREAM_CODEC_RF =
            STREAM_CODEC.cast();
}
