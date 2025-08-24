package net.boulangermod.boulanger;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.entity.*;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        LOGGER.debug("[ModCapabilities] RegisterCapabilitiesEvent fired");

        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                net.boulangermod.boulanger.block.entity.ModBlockEntities.WOOD_GASIFIER_BE.get(),
                (be, side) -> {
                    if (side == null) return null;                // sided
                    if (!be.isFormed()) return null;              // only when formed
                    if (!isPortCell(be)) return null;             // only the port cell BE
                    return side == be.getPortSide() ? be.getPortFluidHandler() : null; // drain-only
                }
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOOD_GAS_PIPE_BE.get(),
                (WoodGasPipeBlockEntity be, @Nullable Direction querySide) -> be.getTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOODGAS_ENGINE_BE.get(),
                (be, side) -> ((WoodGasEngineBlockEntity) be).getFluidHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.WOODGAS_ENGINE_BE.get(),
                (be, side) -> ((WoodGasEngineBlockEntity) be).getEnergyForSide(side) // extract-only view on ALL sides
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_STORAGE_BE.get(),
                (EnergyStorageBlockEntity be, @Nullable Direction querySide) -> be.getEnergyStorage(querySide)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_CABLE_BE.get(),
                (EnergyCableBlockEntity be, @Nullable Direction side) -> be.getEnergyStorage(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.STONE_MILL_BE.get(),
                (StoneMillBlockEntity be, @Nullable Direction side) -> be.getEnergyStorage(side)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOODGAS_FLARE_BE.get(),
                (WoodGasFlareBlockEntity be, @Nullable Direction side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOODGAS_TANK_BE.get(),
                (be, side) -> {
                    if (!(be instanceof net.boulangermod.boulanger.block.entity.WoodGasTankBlockEntity tank)) return null;

                    // Accept from bottom only → expose FILL on LOWER (query side == DOWN)
                    // Push from top only     → expose DRAIN on UPPER (query side == UP)
                    boolean isUpper = tank.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                            == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;

                    if (!isUpper) {
                        // LOWER half: only from DOWN
                        if (side == Direction.DOWN) return tank.bottomFillOnly();
                        return null;
                    } else {
                        // UPPER half: only to UP
                        if (side == Direction.UP) return tank.topDrainOnly();
                        return null;
                    }
                }
        );

        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WALL_FEED_THROUGH_BE.get(),
                (be, side) -> ((WoodGasFeedThroughBlockEntity) be).getHandlerFor(side)
        );


        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOODGAS_VALVE_BE.get(),
                (be, side) -> ((WoodGasValveBlockEntity) be).getFluidHandler(side)
        );
    }

    static boolean isPortCell(net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity be) {
        if (!be.isFormed()) return false;
        var level  = be.getLevel();
        var anchor = be.getMinCorner();
        var state  = be.getBlockState();
        var facing = state.getValue(net.boulangermod.boulanger.block.WoodGasifierBlock.FACING);
        var port   = be.getPortSide();

        for (var cell : net.boulangermod.boulanger.block.entity.WoodGasifierBlockEntity.footprintFromMin(anchor, facing)) {
            var outside = cell.relative(port);
            if (!be.isInFootprint(outside)) {
                return be.getBlockPos().equals(cell); // this BE is the port cell
            }
        }
        return be.isAnchor(); // fallback
    }


}
