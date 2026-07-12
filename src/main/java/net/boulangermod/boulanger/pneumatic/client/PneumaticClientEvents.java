package net.boulangermod.boulanger.pneumatic.client;

import net.boulangermod.boulanger.Config;
import net.boulangermod.boulanger.pneumatic.blockentity.AirCompressorBlockEntity;
import net.boulangermod.boulanger.pneumatic.blockentity.AirTankBlockEntity;
import net.boulangermod.boulanger.pneumatic.blockentity.PneumaticDuctBlockEntity;
import net.boulangermod.boulanger.pneumatic.debug.PneumaticDebug;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Locale;

/**
 * Client-only helper: shows an actionbar overlay while you look at a pneumatic duct.
 */
@EventBusSubscriber(modid = "boulanger", value = Dist.CLIENT)
public final class PneumaticClientEvents {

    private PneumaticClientEvents() { }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        if (!PneumaticDebug.overlayEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.isPaused()) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            mc.player.displayClientMessage(Component.empty(), true);
            return;
        }

        BlockPos pos = bhr.getBlockPos();
        int maxDist = Math.max(1, Config.debugPneumaticOverlayMaxDistance);
        double distSqr = mc.player.distanceToSqr(Vec3.atCenterOf(pos));
        if (distSqr > (double) maxDist * (double) maxDist) {
            mc.player.displayClientMessage(Component.empty(), true);
            return;
        }

        BlockEntity be = mc.level.getBlockEntity(pos);

        String msg;

        if (be instanceof PneumaticDuctBlockEntity duct) {
            msg = String.format(
                    Locale.ROOT,
                    "Duct %d,%d,%d | net=%d | P=%.2fkPa | V=%.2fL | ducts=%d | endpoints=%d | %s",
                    pos.getX(), pos.getY(), pos.getZ(),
                    duct.dbgNetId(),
                    duct.dbgPressureKpa(),
                    duct.dbgTotalVolumeLiters(),
                    duct.dbgDuctCount(),
                    duct.dbgEndpointCount(),
                    duct.blockedSummary()
            );
        } else if (be instanceof AirTankBlockEntity tank) {
            Direction facing = tank.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? tank.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : null;

            msg = String.format(
                    Locale.ROOT,
                    "Tank %d,%d,%d | net=%d | P=%.2fkPa | V=%.2fL | PV=%.2f | ducts=%d | endpoints=%d | port=%s",
                    pos.getX(), pos.getY(), pos.getZ(),
                    tank.getAirNetworkId(),
                    tank.dbgPressureKpa(),
                    tank.dbgTotalVolumeLiters(),
                    tank.dbgPvKpaLiters(),
                    tank.dbgDuctCount(),
                    tank.dbgEndpointCount(),
                    (facing == null ? "?" : facing.getName())
            );
        } else if (be instanceof AirCompressorBlockEntity comp) {
            Direction facing = comp.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? comp.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : null;

            msg = String.format(
                    Locale.ROOT,
                    "Compressor %d,%d,%d | net=%d | %s | P=%.2fkPa | V=%.2fL | PV=%.2f | +PV/t=%.2f | target=%.0fkPa | ducts=%d | endpoints=%d | port=%s",
                    pos.getX(), pos.getY(), pos.getZ(),
                    comp.getAirNetworkId(),
                    (comp.dbgPowered() ? "ON" : "OFF"),
                    comp.dbgPressureKpa(),
                    comp.dbgTotalVolumeLiters(),
                    comp.dbgPvKpaLiters(),
                    comp.dbgLastPvAdded(),
                    comp.dbgTargetKpa(),
                    comp.dbgDuctCount(),
                    comp.dbgEndpointCount(),
                    (facing == null ? "?" : facing.getName())
            );
        } else {
            mc.player.displayClientMessage(Component.empty(), true);
            return;
        }
        mc.player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
    }
}
