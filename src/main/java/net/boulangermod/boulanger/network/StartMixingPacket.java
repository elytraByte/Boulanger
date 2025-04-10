package net.boulangermod.boulanger.network;

import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record StartMixingPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<StartMixingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "start_mixing"));

    //
    // 1) BlockPos codec: wrap FriendlyByteBuf.readBlockPos() / writeBlockPos()
    //
    private static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS_CODEC =
            StreamCodec.of(
                    // encoder: write a BlockPos into the buffer
                    (buf, pos) -> {
                        new FriendlyByteBuf(buf).writeBlockPos(pos);
                    },
                    // decoder: read a BlockPos out of the buffer
                    buf -> new FriendlyByteBuf(buf).readBlockPos()
            );



    //
    // 2) Composite payload codec: just one field, the BlockPos
    //
    public static final StreamCodec<ByteBuf, StartMixingPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BLOCK_POS_CODEC,         // codec for BlockPos
                    StartMixingPacket::pos, // getter
                    StartMixingPacket::new  // constructor
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    //
    // 3) Registration helper: call this in your mod init
    //
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        // "1" is your protocol version string
        PayloadRegistrar registrar = event.registrar("1")
                .executesOn(HandlerThread.NETWORK);

        registrar.playToServer(
                StartMixingPacket.TYPE,
                StartMixingPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        /* client handler */ null,
                        /* server handler */ (StartMixingPacket payload, IPayloadContext ctx) -> {
                    Player sender = ctx.player();
                    if (sender == null) return;
                    Level level = sender.level();
                    BlockEntity be = level.getBlockEntity(payload.pos());
                    if (be instanceof MixingBlockEntity mixer) {
                        mixer.startMixing();
                    }
                }
                )
        );
    }

    //
    // 4) Client helper to send it
    //
    public static void sendFromClient(BlockPos pos) {
        PacketDistributor.sendToServer(new StartMixingPacket(pos));
    }



}


