package net.boulangermod.boulanger.network;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.entity.DoughDividerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetDividerModePacket(BlockPos pos, boolean roll) implements CustomPacketPayload {

    public static final Type<SetDividerModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "set_divider_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDividerModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetDividerModePacket::pos,
                    ByteBufCodecs.BOOL, SetDividerModePacket::roll,
                    SetDividerModePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server handler */
    public static void handle(final SetDividerModePacket msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null) return;
            var level = player.level();
            if (level == null || level.isClientSide()) return;

            var be = level.getBlockEntity(msg.pos());
            if (be instanceof DoughDividerBlockEntity divider) {
                divider.setRollMode(msg.roll());
            }
        });
    }
}
