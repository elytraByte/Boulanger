package net.boulangermod.boulanger.util;

import com.mojang.serialization.Codec;
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

public final class StreamCodecsCompat {
    private StreamCodecsCompat() {}

    public static final StreamCodec<RegistryFriendlyByteBuf, String> STRING =
            StreamCodec.of(RegistryFriendlyByteBuf::writeUtf, RegistryFriendlyByteBuf::readUtf);

    public static final StreamCodec<RegistryFriendlyByteBuf, Float> FLOAT =
            StreamCodec.of(RegistryFriendlyByteBuf::writeFloat, RegistryFriendlyByteBuf::readFloat);

    public static final StreamCodec<RegistryFriendlyByteBuf, Double> DOUBLE =
            StreamCodec.of(RegistryFriendlyByteBuf::writeDouble, RegistryFriendlyByteBuf::readDouble);

    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT =
            StreamCodec.of(RegistryFriendlyByteBuf::writeInt, RegistryFriendlyByteBuf::readInt);

    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> VAR_INT =
            StreamCodec.of(RegistryFriendlyByteBuf::writeVarInt, RegistryFriendlyByteBuf::readVarInt);

    public static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOLEAN =
            StreamCodec.of(RegistryFriendlyByteBuf::writeBoolean, RegistryFriendlyByteBuf::readBoolean);

    public static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM =
            ByteBufCodecs.registry(Registries.ITEM);

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceLocation> RESOURCE_LOCATION =
            StreamCodec.of(RegistryFriendlyByteBuf::writeResourceLocation, RegistryFriendlyByteBuf::readResourceLocation);

    public static <K, V> Codec<Map<K, V>> mapCodec(Codec<K> keyCodec, Codec<V> valueCodec) {
        return Codec.unboundedMap(keyCodec, valueCodec);
    }

    public static <K, V> StreamCodec<RegistryFriendlyByteBuf, Map<K, V>> map(
            StreamCodec<? super RegistryFriendlyByteBuf, K> keyCodec,
            StreamCodec<? super RegistryFriendlyByteBuf, V> valueCodec
    ) {
        return StreamCodec.of(
                (buf, map) -> {
                    buf.writeVarInt(map.size());
                    for (var entry : map.entrySet()) {
                        keyCodec.encode(buf, entry.getKey());
                        valueCodec.encode(buf, entry.getValue());
                    }
                },
                buf -> {
                    int size = buf.readVarInt();
                    Map<K, V> map = new HashMap<>(Math.max(16, size * 2));
                    for (int i = 0; i < size; i++) {
                        map.put(keyCodec.decode(buf), valueCodec.decode(buf));
                    }
                    return map;
                }
        );
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, List<T>> list(
            StreamCodec<? super RegistryFriendlyByteBuf, T> elementCodec
    ) {
        return StreamCodec.of(
                (buf, list) -> {
                    buf.writeVarInt(list.size());
                    for (T element : list) elementCodec.encode(buf, element);
                },
                buf -> {
                    int size = buf.readVarInt();
                    List<T> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) list.add(elementCodec.decode(buf));
                    return list;
                }
        );
    }
}
