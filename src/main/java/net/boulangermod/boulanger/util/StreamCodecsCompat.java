package net.boulangermod.boulanger.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.Map;

public class StreamCodecsCompat {
    public static final StreamCodec<RegistryFriendlyByteBuf, String> STRING =
            StreamCodec.of(RegistryFriendlyByteBuf::writeUtf, RegistryFriendlyByteBuf::readUtf);

    public static final StreamCodec<RegistryFriendlyByteBuf, Float> FLOAT =
            StreamCodec.of(RegistryFriendlyByteBuf::writeFloat, RegistryFriendlyByteBuf::readFloat);

    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT =
            StreamCodec.of(RegistryFriendlyByteBuf::writeInt, RegistryFriendlyByteBuf::readInt);

    public static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM =
            ByteBufCodecs.registry(Registries.ITEM);

    public static <K, V> Codec<Map<K, V>> mapCodec(Codec<K> keyCodec, Codec<V> valueCodec) {
        // unboundedMap allows arbitrary map sizes, serializing as JSON objects
        return Codec.unboundedMap(keyCodec, valueCodec);
    }



}
