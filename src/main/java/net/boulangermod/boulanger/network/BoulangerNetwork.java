package net.boulangermod.boulanger.network;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class BoulangerNetwork {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                StartMixingPacket.TYPE,
                StartMixingPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player().level().getBlockEntity(payload.pos()) instanceof MixingBlockEntity mixer)) {
                            return;
                        }
                        // Call your custom logic for mixing.
                        mixer.startMixing();
                    });
                }
        );

        registrar.playBidirectional(
                MeasureData.TYPE,
                MeasureData.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        // Client payload handler (if needed, otherwise can be left empty)
                        (data, context) -> { /* client-side no-op or logging */ },
                        // Server payload handler (calls your server logic)
                        ServerPayloadHandler::handleDataOnMain
                )
        );
    }

    /**
     * Sends a payload from the client to the server.
     *
     * @param payload The payload to send.
     * @param <T>     A type that extends CustomPacketPayload.
     */
    public static <T extends CustomPacketPayload> void sendToServer(T payload) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }
}
