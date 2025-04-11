package net.boulangermod.boulanger.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Boulanger.MODID);

    public static final Supplier<BlockEntityType<WoodGasifierBlockEntity>> WOOD_GASIFIER_BE =
            BLOCK_ENTITIES.register("wood_gasifier_be", () -> BlockEntityType.Builder.of(
                    WoodGasifierBlockEntity::new, ModBlocks.WOOD_GASIFIER.get()).build(null));

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

//    public static final Supplier<BlockEntityType<MixingBlockEntity>> MIXING_BLOCK_BE =
//            BLOCK_ENTITIES.register("mixing_block_be", () -> BlockEntityType.Builder.of(
//                    MixingBlockEntity::new, ModBlocks.MIXING_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}