package net.boulangermod.boulanger.network;

import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.screen.MilligramScaleMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Sent from client → server when the player clicks “Measure” on the pocket scale.
 */
public record MeasureItemData(int containerId, int weightMg) implements CustomPacketPayload {
    public static final Type<MeasureItemData> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "measure_item"));

    /**
     * Encode/decode both fields in one go.
     */
    public static final StreamCodec<ByteBuf, MeasureItemData> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        FriendlyByteBuf fb = new FriendlyByteBuf(buf);
                        fb.writeVarInt(pkt.containerId());
                        fb.writeVarInt(pkt.weightMg());
                    },
                    buf -> {
                        FriendlyByteBuf fb = new FriendlyByteBuf(buf);
                        int id = fb.readVarInt();
                        int mg = fb.readVarInt();
                        return new MeasureItemData(id, mg);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Register this payload handler; call ModDataComponentTypes.register(...) in your mod init,
     * and make sure this class is on the event bus.
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1")
                .executesOn(HandlerThread.NETWORK);

        registrar.playToServer(
                MeasureItemData.TYPE,
                MeasureItemData.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        /* client: */  null,
                        /* server: */ (MeasureItemData payload, IPayloadContext ctx) -> {
                    Player sender = ctx.player();
                    if (!(sender instanceof ServerPlayer server)) return;
                    Level level = server.level();
                    AbstractContainerMenu menu = server.containerMenu;
                    if (menu instanceof MilligramScaleMenu scaleMenu
                            && scaleMenu.containerId == payload.containerId()) {
                        // perform the transfer
                        scaleMenu.onMeasureClick(payload.weightMg());
                    }
                }
                )
        );
    }

    /**
     * Helper to call from client when you click Measure:
     */
    public static void sendFromClient(int containerId, int weightMg) {
        PacketDistributor.sendToServer(new MeasureItemData(containerId, weightMg));
    }
}
