package net.boulangermod.boulanger.entity;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Boulanger.MODID);

    public static final Supplier<EntityType<HenEntity>> HEN =
            ENTITY_TYPES.register("hen", () ->
                    EntityType.Builder.of(HenEntity::new, MobCategory.CREATURE)
                            .sized(0.4f, 0.7f)  // same as vanilla chicken
                            .build("hen"));


    public static final Supplier<EntityType<HefferEntity>> HEFFER =
            ENTITY_TYPES.register("heffer", () ->
                    EntityType.Builder.of(HefferEntity::new, MobCategory.CREATURE)
                            .sized(0.9f, 1.4f) // same size as cow
                            .build("heffer"));



    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

}
