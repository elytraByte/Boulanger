package net.boulangermod.boulanger.content.pan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.Boulanger;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PanType {
    LOAF(rl("loaf"), 1, 1, 2, rl("loaf_pan")),
    BAGUETTE(rl("baguette"), 3, 3, 4, rl("baguette_pan"));

    private final ResourceLocation id;
    private final int capacity;
    private final int emptyModelIndex;
    private final int fullModelIndex;
    private final ResourceLocation assetKey;

    PanType(ResourceLocation id, int capacity, int emptyModelIndex, int fullModelIndex, ResourceLocation assetKey) {
        this.id = id;
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0 for " + id);
        if (emptyModelIndex < 0) throw new IllegalArgumentException("emptyModelIndex must be >= 0 for " + id);
        if (fullModelIndex < 0) throw new IllegalArgumentException("fullModelIndex must be >= 0 for " + id);
        this.capacity = capacity;
        this.emptyModelIndex = emptyModelIndex;
        this.fullModelIndex = fullModelIndex;
        this.assetKey = assetKey;
    }

    private static final Map<ResourceLocation, PanType> BY_ID =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(PanType::id, Function.identity()));

    public ResourceLocation id() { return id; }
    public String idString() { return id.toString(); }

    public int capacity() { return capacity; }
    public int emptyModelIndex() { return emptyModelIndex; }
    public int fullModelIndex() { return fullModelIndex; }
    public ResourceLocation assetKey() { return assetKey; }

    public static @Nullable PanType byId(ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** Accept namespaced ("boulanger:loaf") OR bare ("loaf"). */
    public static @Nullable PanType byId(String raw) {
        if (raw == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        if (rl == null) {
            rl = ResourceLocation.fromNamespaceAndPath(Boulanger.MOD_ID, raw.toLowerCase(Locale.ROOT));
        }
        return BY_ID.get(rl);
    }

    public static final Codec<PanType> CODEC =
            ResourceLocation.CODEC.flatXmap(
                    rl -> {
                        PanType t = byId(rl);
                        return t != null
                                ? DataResult.success(t)
                                : DataResult.error(() -> "Unknown PanType id: " + rl);
                    },
                    t -> DataResult.success(t.id())
            );

    public static final StreamCodec<ByteBuf, PanType> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    s -> {
                        PanType t = byId(s);
                        if (t == null) throw new IllegalArgumentException("Unknown PanType id: " + s);
                        return t;
                    },
                    PanType::idString
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, PanType> STREAM_CODEC_RF =
            STREAM_CODEC.cast();

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Boulanger.MOD_ID, path);
    }
}
