package org.l3e.boulanger.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Boulanger.MODID);

    public static final Supplier<BlockEntityType<MixerBlockEntity>> MIXER_BE =
            BLOCK_ENTITIES.register("mixer_be", () -> BlockEntityType.Builder.of(
                    MixerBlockEntity::new, ModBlocks.MIXER.get()).build(null));

    public static final Supplier<BlockEntityType<WoodGasifierBlockEntity>> WOOD_GASIFIER_BE =
            BLOCK_ENTITIES.register("wood_gasifier_be", () -> BlockEntityType.Builder.of(
                    WoodGasifierBlockEntity::new, ModBlocks.WOOD_GASIFIER.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}