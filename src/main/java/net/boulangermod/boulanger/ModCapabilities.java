package net.boulangermod.boulanger;

import net.boulangermod.boulanger.block.entity.*;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;

@EventBusSubscriber(
        modid = Boulanger.MODID,
        bus   = EventBusSubscriber.Bus.MOD   // ← MOD, not GAME
)
public class ModCapabilities {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        LOGGER.debug("[ModCapabilities] RegisterCapabilitiesEvent fired");

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOOD_GAS_PIPE_BE.get(),
                (WoodGasPipeBlockEntity be, Direction side) -> be.getTank()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WOOD_GASIFIER_BE.get(),
                (WoodGasifierBlockEntity be, Direction side) -> be.getFluidHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_STORAGE_BE.get(),
                (EnergyStorageBlockEntity be, Direction side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.INTERNAL_COMBUSTION_ENGINE_BE.get(),
                (InternalCombustionEngineBlockEntity be, @Nullable Direction side) -> be.getFluidHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.INTERNAL_COMBUSTION_ENGINE_BE.get(),
                (InternalCombustionEngineBlockEntity be, @Nullable Direction side) -> be.getEnergyStorage(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_STORAGE_BE.get(),
                (EnergyStorageBlockEntity be, Direction side) -> be.getEnergyStorage(side)
        );
    }
}
