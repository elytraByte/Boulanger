package net.boulangermod.component;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.Boulanger;

import java.util.function.UnaryOperator;

public class ModDataComponentTypes {
    // 1. Create a ResourceKey for your DataComponent registry
    public static final ResourceKey<Registry<DataComponentType<?>>> DATA_COMPONENT_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "data_components")
            );

    // 2. Use the key in DeferredRegister creation
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(DATA_COMPONENT_REGISTRY_KEY, Boulanger.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WheatType>> WHEAT_TYPE
            = register("wheat_type", builder -> builder.persistent(WheatType.CODEC));

    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
