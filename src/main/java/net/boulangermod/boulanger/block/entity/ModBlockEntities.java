package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.multiblock.TestMultiblockMasterBlockEntity;
import net.boulangermod.boulanger.multiblock.TestMultiblockSlaveBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Boulanger.MODID);

    public static final Supplier<BlockEntityType<WoodOvenBlockEntity>> WOOD_OVEN_BE =
            BLOCK_ENTITIES.register("wood_oven_be", () -> BlockEntityType.Builder.of(
                    WoodOvenBlockEntity::new, ModBlocks.WOOD_OVEN.get()).build(null));

    public static final Supplier<BlockEntityType<MixingBlockEntity>> MIXING_BLOCK_BE =
            BLOCK_ENTITIES.register("mixing_block_be", () -> BlockEntityType.Builder.of(
                    MixingBlockEntity::new, ModBlocks.MIXING_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<ScaleBlockEntity>> SCALE_BLOCK_BE =
            BLOCK_ENTITIES.register("scale_block_be", () -> BlockEntityType.Builder.of(
                    ScaleBlockEntity::new, ModBlocks.SCALE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<StoneMillBlockEntity>> STONE_MILL_BE =
            BLOCK_ENTITIES.register("stone_mill_block_be", () -> BlockEntityType.Builder.of(
                    StoneMillBlockEntity::new, ModBlocks.STONE_MILL_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<TreeTapBlockEntity>> TREE_TAP =
            BLOCK_ENTITIES.register("tree_tap", () -> BlockEntityType.Builder.of(
                    TreeTapBlockEntity::new, ModBlocks.TREE_TAP.get()
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<WoodGasifierBlockEntity>> WOOD_GASIFIER_BE =
            BLOCK_ENTITIES.register("wood_gasifier_be", () -> BlockEntityType.Builder.of(
                    WoodGasifierBlockEntity::new,
                    ModBlocks.WOOD_GASIFIER.get()
            ).build(null));

    public static final Supplier<BlockEntityType<TestMultiblockMasterBlockEntity>> TEST_MULTIBLOCK_MASTER_BE =
            BLOCK_ENTITIES.register("test_multiblock_master",
                    () -> BlockEntityType.Builder
                            .of(TestMultiblockMasterBlockEntity::new,
                                    ModBlocks.TEST_MULTIBLOCK_MASTER.get())
                            .build(null)
            );

    public static final Supplier<BlockEntityType<TestMultiblockSlaveBlockEntity>> TEST_MULTIBLOCK_SLAVE_BE =
            BLOCK_ENTITIES.register("test_multiblock_slave",
                    () -> BlockEntityType.Builder
                            .of(TestMultiblockSlaveBlockEntity::new,
                                    ModBlocks.TEST_MULTIBLOCK_SLAVE.get())
                            .build(null)
            );

    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_BE =
            BLOCK_ENTITIES.register("energy_storage_be", () -> BlockEntityType.Builder.of(
                    EnergyStorageBlockEntity::new,
                    ModBlocks.BATTERY.get()
            ).build(null));

    public static final Supplier<BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE_BE =
            BLOCK_ENTITIES.register("energy_cable_be", () -> BlockEntityType.Builder.of(
                    EnergyCableBlockEntity::new,
                    ModBlocks.ENERGY_CABLE.get()
            ).build(null));

    public static final Supplier<BlockEntityType<WoodGasPipeBlockEntity>> WOOD_GAS_PIPE_BE =
            BLOCK_ENTITIES.register("woodgas_pipe_be", () -> BlockEntityType.Builder.of(
                    WoodGasPipeBlockEntity::new,
                    ModBlocks.WOODGAS_PIPE.get()
            ).build(null));

    public static final Supplier<BlockEntityType<InternalCombustionEngineBlockEntity>> INTERNAL_COMBUSTION_ENGINE_BE =
            BLOCK_ENTITIES.register("ice_be", () -> BlockEntityType.Builder.of(
                    InternalCombustionEngineBlockEntity::new,
                    ModBlocks.INTERAL_COMUSTION_ENGINE.get()
            ).build(null));

    public static final Supplier<BlockEntityType<ProofingBoxBlockEntity>> PROOFING_BOX =
            BLOCK_ENTITIES.register("proofing_box", () -> BlockEntityType.Builder.of(
                    ProofingBoxBlockEntity::new,
                    ModBlocks.PROOFING_BOX.get()
            ).build(null));

    public static final Supplier<BlockEntityType<BakersTableBlockEntity>> BAKERS_TABLE =
            BLOCK_ENTITIES.register("bakers_table", () -> BlockEntityType.Builder.of(
                    BakersTableBlockEntity::new,
                    ModBlocks.BAKERS_TABLE.get()
            ).build(null));

    public static final Supplier<BlockEntityType<DoughDividerBlockEntity>> DOUGH_DIVIDER =
            BLOCK_ENTITIES.register("dough_divider", () -> BlockEntityType.Builder.of(
                    DoughDividerBlockEntity::new,
                    ModBlocks.DOUGH_DIVIDER.get()
            ).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}