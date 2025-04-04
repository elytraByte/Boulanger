package net.boulangermod.block.entity;

import net.boulangermod.Boulanger;
import net.boulangermod.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set; // Keep Set import

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Boulanger.MODID);

//    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodGasifierBlockEntity>> WOOD_GASIFIER_BE =
//            BLOCK_ENTITIES.register("wood_gasifier_be", () ->
//                    // Use the TWO-argument constructor: (Supplier, Set)
//                    new BlockEntityType<>(
//                            WoodGasifierBlockEntity::new,      // BlockEntitySupplier
//                            Set.of(ModBlocks.WOOD_GASIFIER.get())  // Set of valid Block(s)
//                            // REMOVED the third 'null' argument
//                    )
//            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}