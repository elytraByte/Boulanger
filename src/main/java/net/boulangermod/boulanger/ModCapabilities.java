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

        /* ── Wood Gasifier: expose ONLY the port on the anchor when formed ── */
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOOD_GASIFIER_BE.get(),
                (WoodGasifierBlockEntity be, @Nullable Direction querySide) -> {
                    if (!be.isFormed() || !be.isAnchor()) return null;

                    Direction port = be.getPortSide();

                    // If you do visual checks with side == null, expose so the probe can “see” it
                    if (querySide == null) return be.getPortFluidHandler();

                    // Real sided exposure
                    return (querySide == port) ? be.getPortFluidHandler() : null;
                }
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOOD_GAS_PIPE_BE.get(),
                (WoodGasPipeBlockEntity be, @Nullable Direction querySide) -> be.getTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.INTERNAL_COMBUSTION_ENGINE_BE.get(),
                (InternalCombustionEngineBlockEntity be, @Nullable Direction querySide) -> be.getFluidHandler(querySide)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.INTERNAL_COMBUSTION_ENGINE_BE.get(),
                (InternalCombustionEngineBlockEntity be, @Nullable Direction querySide) -> be.getEnergyStorage(querySide)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_STORAGE_BE.get(),
                (EnergyStorageBlockEntity be, @Nullable Direction querySide) -> be.getEnergyStorage(querySide)
        );
    }
}
