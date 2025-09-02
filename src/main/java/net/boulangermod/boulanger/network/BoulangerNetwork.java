package net.boulangermod.boulanger.network;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.boulangermod.boulanger.screen.MilligramScaleMenu;
import net.boulangermod.boulanger.screen.ScaleBlockMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.world.inventory.AbstractContainerMenu;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class BoulangerNetwork {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // ——— start mixing (unchanged) ———
        registrar.playToServer(
                StartMixingPacket.TYPE,
                StartMixingPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player().level()
                            .getBlockEntity(payload.pos())
                            instanceof MixingBlockEntity mixer) {
                        mixer.startMixing();
                    }
                })
        );

        registrar.playToServer(
                MeasureItemData.TYPE,
                MeasureItemData.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer server)) return;
                    AbstractContainerMenu menu = server.containerMenu;

                    // 1) Item‐scale menu
                    if (menu instanceof MilligramScaleMenu itemMenu
                            && itemMenu.getContainerId() == payload.containerId()) {
                        itemMenu.onMeasureClick(payload.weightMg());
                        return;
                    }

                    // 2) Block‐scale menu
                    if (menu instanceof ScaleBlockMenu blockMenu
                            && blockMenu.getContainerId() == payload.containerId()) {
                        blockMenu.onMeasureClick(payload.weightMg());
                    }
                })
        );

        registrar.playToServer(
                ClearMixerPacket.TYPE,
                ClearMixerPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof net.boulangermod.boulanger.block.entity.MixingBlockEntity mixer) {
                        mixer.clearAndEject(context.player()); // see method below
                    }
                })
        );
    }

    public static <T extends CustomPacketPayload> void sendToServer(T payload) {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null && conn.hasChannel(payload.type().id())) {
            PacketDistributor.sendToServer(payload);
        }
    }
}