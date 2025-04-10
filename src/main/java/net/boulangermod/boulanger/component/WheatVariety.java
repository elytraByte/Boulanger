package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record WheatVariety(ResourceLocation id) {
    public static final Codec<WheatVariety> CODEC = ResourceLocation.CODEC.xmap(WheatVariety::new, WheatVariety::id);

    public static final StreamCodec<RegistryFriendlyByteBuf, WheatVariety> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, variety -> variety.id().toString(),
                    idStr -> new WheatVariety(ResourceLocation.tryParse(idStr))
            );
}
