package net.boulangermod.boulanger.network;

import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.boulangermod.boulanger.block.entity.ScaleBlockEntity;
import net.boulangermod.boulanger.network.StartMixingPacket;
import net.boulangermod.boulanger.network.MeasureData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleStartMixing(StartMixingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();

            if (level.getBlockEntity(packet.pos()) instanceof MixingBlockEntity mixer) {
                mixer.startMixing();
            } else {
                context.disconnect(Component.literal("Invalid mixing block position"));
            }
        });
    }

    public static void handleDataOnMain(MeasureData data, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(data.pos());

            if (be instanceof ScaleBlockEntity scaleEntity) {
                scaleEntity.setGramsToWeigh(data.weight());
                scaleEntity.transferToBowl();
            } else {
                context.disconnect(Component.literal("Invalid scale block position"));
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("boulangermod.network.failed", e.getMessage()));
            return null;
        });
    }
}
