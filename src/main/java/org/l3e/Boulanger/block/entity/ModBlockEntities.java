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
    public static final RegistryObject<BlockEntityType<ThreshingMachineBlockEntity>> THRESHING_MACHINE =
            BLOCK_ENTITIES.register("threshing_machine", () ->
                    BlockEntityType.Builder.of(ThreshingMachineBlockEntity::new,
                            ModBlocks.THRESHING_MACHINE.get()).build(null));
    public static final RegistryObject<BlockEntityType<SeparatorBlockEntity>> SEPARATOR =
            BLOCK_ENTITIES.register("separator", () ->
                    BlockEntityType.Builder.of(SeparatorBlockEntity::new,
                            ModBlocks.SEPARATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<AspiratorBlockEntity>> ASPIRATOR =
            BLOCK_ENTITIES.register("aspirator", () ->
                    BlockEntityType.Builder.of(AspiratorBlockEntity::new,
                            ModBlocks.ASPIRATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<DestonerBlockEntity>> DESTONER =
            BLOCK_ENTITIES.register("destoner", () ->
                    BlockEntityType.Builder.of(DestonerBlockEntity::new,
                            ModBlocks.DESTONER.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
