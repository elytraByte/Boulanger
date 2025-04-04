package net.boulangermod.boulanger.util;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class StreamCodecsCompat {
    public static final StreamCodec<RegistryFriendlyByteBuf, String> STRING =
            StreamCodec.of(RegistryFriendlyByteBuf::writeUtf, RegistryFriendlyByteBuf::readUtf);

    public static final StreamCodec<RegistryFriendlyByteBuf, Float> FLOAT =
            StreamCodec.of(RegistryFriendlyByteBuf::writeFloat, RegistryFriendlyByteBuf::readFloat);

    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT =
            StreamCodec.of(RegistryFriendlyByteBuf::writeInt, RegistryFriendlyByteBuf::readInt);

}
