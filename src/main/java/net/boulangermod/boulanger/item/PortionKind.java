package net.boulangermod.boulanger.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

public enum PortionKind {
    LOAF, ROLL, BAGUETTE; // etc.

    // JSON codec (if you need it)
    public static final Codec<PortionKind> CODEC =
            Codec.STRING.xmap(s -> PortionKind.valueOf(s.toUpperCase(Locale.ROOT)), PortionKind::name);

    // NETWORK codec (ByteBuf-typed)
    public static final StreamCodec<ByteBuf, PortionKind> STREAM_CODEC =
            ByteBufCodecs.idMapper(
                    i -> PortionKind.values()[i],   // decode from ordinal
                    PortionKind::ordinal            // encode to ordinal
            );

    // Optional alias if some API wants RegistryFriendlyByteBuf
    public static final StreamCodec<RegistryFriendlyByteBuf, PortionKind> STREAM_CODEC_RF =
            STREAM_CODEC.cast();
}