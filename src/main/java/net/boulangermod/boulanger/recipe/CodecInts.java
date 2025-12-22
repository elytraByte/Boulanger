package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class CodecInts {
    private CodecInts() {}

    public static final Codec<Integer> POSITIVE =
            Codec.INT.xmap(i -> Math.max(1, i), i -> Math.max(1, i));

    // ByteBuf-typed stream codec
    public static final StreamCodec<ByteBuf, Integer> POSITIVE_STREAM =
            ByteBufCodecs.VAR_INT.map(i -> Math.max(1, i), i -> Math.max(1, i));

    // If/when a RF buffer is required:
    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> POSITIVE_STREAM_RF =
            POSITIVE_STREAM.cast();
}