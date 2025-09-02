package net.boulangermod.boulanger.network;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClearMixerPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ClearMixerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "clear_mixer"));

    // 1.21 style: compose from the BlockPos codec
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearMixerPacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, ClearMixerPacket::pos, ClearMixerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // convenience
    public static void sendFromClient(BlockPos pos) {
        BoulangerNetwork.sendToServer(new ClearMixerPacket(pos));
    }
}
