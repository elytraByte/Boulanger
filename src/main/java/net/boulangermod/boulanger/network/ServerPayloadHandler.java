package net.boulangermod.boulanger.network;

import net.boulangermod.boulanger.block.entity.MixingBlockEntity;
import net.boulangermod.boulanger.block.entity.ScaleBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class ServerPayloadHandler {
    public static void handleStartMixing(StartMixingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player(); // always safe on serverbound packets
            Level level = player.level();

            if (level.getBlockEntity(packet.pos()) instanceof MixingBlockEntity mixer) {
                mixer.startMixing(); // your custom logic
            } else {
                context.disconnect(Component.literal("Invalid mixing block position"));
            }
        });
    }

    public static void handleDataOnMain(final MeasureData data, final IPayloadContext context) {
        // Enqueue work on the main thread so you can safely interact with game objects.
        context.enqueueWork(() -> {
            // Retrieve the sender of the payload.
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null) {
                Level level = player.level();
                BlockEntity be = level.getBlockEntity(data.pos());
                if (be instanceof ScaleBlockEntity scaleEntity) {
                    // Set the weight to transfer and execute the transfer logic.
                    scaleEntity.setGramsToWeigh(data.weight());
                    scaleEntity.transferToBowl();
                    System.out.println("Received weight: " + data.weight());
                }
            }
        }).exceptionally(e -> {
            // Handle any exceptions that occur on the main thread.
            context.disconnect(Component.translatable("boulangermod.network.failed", e.getMessage()));
            return null;
        });
    }
}


