package net.boulangermod.boulanger.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceLocation> RESOURCE_LOCATION =
            StreamCodec.of(RegistryFriendlyByteBuf::writeResourceLocation, RegistryFriendlyByteBuf::readResourceLocation);

    public static final StreamCodec<RegistryFriendlyByteBuf, Double> DOUBLE =
            StreamCodec.of(RegistryFriendlyByteBuf::writeDouble, RegistryFriendlyByteBuf::readDouble);





    // --- JSON Codec for maps (used in RecordCodecBuilder) ---
    public static <K, V> Codec<Map<K, V>> mapCodec(Codec<K> keyCodec, Codec<V> valueCodec) {
        return Codec.unboundedMap(keyCodec, valueCodec);
    }

    // --- StreamCodec for maps (used in binary serialization) ---
    public static <K, V> StreamCodec<RegistryFriendlyByteBuf, Map<K, V>> map(
            StreamCodec<? super RegistryFriendlyByteBuf, K> keyCodec,
            StreamCodec<? super RegistryFriendlyByteBuf, V> valueCodec
    ) {
        return StreamCodec.of(
                (buf, map) -> {
                    buf.writeVarInt(map.size());
                    for (Map.Entry<K, V> entry : map.entrySet()) {
                        keyCodec.encode(buf, entry.getKey());
                        valueCodec.encode(buf, entry.getValue());
                    }
                },
                buf -> {
                    int size = buf.readVarInt();
                    Map<K, V> map = new HashMap<>();
                    for (int i = 0; i < size; i++) {
                        K key = keyCodec.decode(buf);
                        V value = valueCodec.decode(buf);
                        map.put(key, value);
                    }
                    return map;
                }
        );
    }

    // --- StreamCodec for lists (used in binary serialization) ---
    public static <T> StreamCodec<RegistryFriendlyByteBuf, List<T>> list(
            StreamCodec<? super RegistryFriendlyByteBuf, T> elementCodec
    ) {
        return StreamCodec.of(
                (buf, list) -> {
                    buf.writeVarInt(list.size());
                    for (T element : list) {
                        elementCodec.encode(buf, element);
                    }
                },
                buf -> {
                    int size = buf.readVarInt();
                    List<T> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(elementCodec.decode(buf));
                    }
                    return list;
                }
        );
    }
}
