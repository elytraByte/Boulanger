package net.boulangermod.boulanger.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


// This record represents the data that will be sent from client to server.
public record MeasureData(int weight, BlockPos pos) implements CustomPacketPayload {

    // Unique identifier for this payload type.
    public static final CustomPacketPayload.Type<MeasureData> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("boulangermod", "measure_data"));

    // A StreamCodec that serializes the weight and the BlockPos.
    // Here we encode the weight as a VAR_INT and the BlockPos's x, y, and z components as VAR_INT values.
    public static final StreamCodec<ByteBuf, MeasureData> STREAM_CODEC = StreamCodec.composite(
            // First field: weight (VAR_INT)
            ByteBufCodecs.VAR_INT, MeasureData::weight,
            // Next fields: the BlockPos components (each as a VAR_INT)
            ByteBufCodecs.VAR_INT, data -> data.pos().getX(),
            ByteBufCodecs.VAR_INT, data -> data.pos().getY(),
            ByteBufCodecs.VAR_INT, data -> data.pos().getZ(),
            // Constructor: rebuild MeasureData from the decoded values.
            (weight, x, y, z) -> new MeasureData(weight, new BlockPos(x, y, z))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
