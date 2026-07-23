package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.pneumatic.blockentity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Boulanger.MOD_ID);

    public static final Supplier<BlockEntityType<TreeTapBlockEntity>> TREE_TAP_BE =
            BLOCK_ENTITIES.register("tree_tap_be", () -> BlockEntityType.Builder.of(
                            TreeTapBlockEntity::new, ModBlocks.TREE_TAP.get()
                    ).build(null)
            );
//pneumatic ducting deprecated for the time being
//    public static final Supplier<BlockEntityType<PneumaticDuctBlockEntity>> PNEUMATIC_DUCT_BE =
//            BLOCK_ENTITIES.register("pneumatic_duct_be", () -> BlockEntityType.Builder.of(
//                        PneumaticDuctBlockEntity::new,
//                    ModBlocks.PNEUMATIC_DUCT.get(),
//                    ModBlocks.VALVE_DUCT.get(),
//                    ModBlocks.ONE_WAY_VALVE_DUCT.get()
//                ).build(null)
//            );
//
//    public static final Supplier<BlockEntityType<AirCompressorBlockEntity>> AIR_COMPRESSOR_BE =
//            BLOCK_ENTITIES.register("air_compressor_be", () -> BlockEntityType.Builder.of(
//                            AirCompressorBlockEntity::new, ModBlocks.AIR_COMPRESSOR.get()
//                    ).build(null)
//            );
//
//    public static final Supplier<BlockEntityType<AirTankBlockEntity>> AIR_TANK_BE =
//            BLOCK_ENTITIES.register("air_tank_be", () -> BlockEntityType.Builder.of(
//                            AirTankBlockEntity::new, ModBlocks.AIR_TANK.get()
//                    ).build(null)
//            );

//    public static final Supplier<BlockEntityType<ValveDuctBlockEntity>> VALVE_DUCT_BE =
//            BLOCK_ENTITIES.register("valve_duct_be", () -> BlockEntityType.Builder.of(
//                            ValveDuctBlockEntity::new, ModBlocks.VALVE_DUCT.get()
//                    ).build(null)
//            );
//
//    public static final Supplier<BlockEntityType<OneWayValveDuctBlockEntity>> ONE_WAY_VALVE_DUCT =
//            BLOCK_ENTITIES.register("one_way_valve_duct_be", () -> BlockEntityType.Builder.of(
//                            OneWayValveDuctBlockEntity::new, ModBlocks.ONE_WAY_VALVE_DUCT.get()
//                    ).build(null)
//            );




    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
