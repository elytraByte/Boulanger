package org.l3e.Boulanger.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.l3e.Boulanger.Boulanger;
import org.l3e.Boulanger.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Boulanger.MOD_ID);

    public static final RegistryObject<BlockEntityType<StoneMillBlockEntity>> STONE_MILL =
            BLOCK_ENTITIES.register("stone_mill", () ->
                    BlockEntityType.Builder.of(StoneMillBlockEntity::new,
                            ModBlocks.STONE_MILL.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
