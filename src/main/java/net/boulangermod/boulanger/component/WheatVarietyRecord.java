package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import net.boulangermod.boulanger.item.WheatVariety;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record WheatVarietyRecord(ResourceLocation id) {
    public static final Codec<WheatVarietyRecord> CODEC = ResourceLocation.CODEC.xmap(
            WheatVarietyRecord::new, WheatVarietyRecord::id);

    public static final StreamCodec<RegistryFriendlyByteBuf, WheatVarietyRecord> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, variety -> variety.id().toString(),
                    idStr -> new WheatVarietyRecord(ResourceLocation.tryParse(idStr))
            );
}
