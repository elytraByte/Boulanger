package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class CodecInts {
    private CodecInts() {}

    // JSON / data codec: validate instead of clamping
    public static final Codec<Integer> POSITIVE = Codec.intRange(1, Integer.MAX_VALUE);

    // NETWORK codec: validate instead of clamping
    public static final StreamCodec<ByteBuf, Integer> POSITIVE_STREAM =
            ByteBufCodecs.VAR_INT.map(CodecInts::requirePositive, CodecInts::requirePositive);

    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> POSITIVE_STREAM_RF =
            POSITIVE_STREAM.cast();

    private static int requirePositive(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Expected positive int, got " + i);
        }
        return i;
    }
}
