package net.boulangermod.boulanger.network;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.boulangermod.boulanger.network.MeasureItemData;
import net.boulangermod.boulanger.screen.MilligramScaleMenu;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;  // <-- import PacketDistributor

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
                        if (!(context.player().level().getBlockEntity(payload.pos())
                                instanceof MixingBlockEntity mixer)) {
                            return;
                        }
                        mixer.startMixing();
                    });
                }
        );

        registrar.playToServer(
                MeasureItemData.TYPE,
                MeasureItemData.STREAM_CODEC,
                new DirectionalPayloadHandler<MeasureItemData>(null, (payload, ctx) -> {
                    // client side (no-op)
                    var sender = ctx.player();
                    if (!(sender instanceof ServerPlayer server)) return;
                    var menu = server.containerMenu;
                    if (menu instanceof MilligramScaleMenu scaleMenu
                            && scaleMenu.containerId == payload.containerId()) {
                        scaleMenu.onMeasureClick(payload.weightMg());
                    }
                })

        );
    }

    /**
     * Sends a payload from the client to the server via the registered channel.
     */
    public static <T extends CustomPacketPayload> void sendToServer(T payload) {
        PacketDistributor.sendToServer(payload);
    }
}
