package net.boulangermod.boulanger.component;


import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.WheatVariety;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.Boulanger;

import java.util.function.UnaryOperator;

public class ModDataComponentTypes {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Boulanger.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WheatVariety>> WHEAT_VARIETY =
            register("wheat_variety", builder -> builder
                    .persistent(WheatVariety.CODEC)
                    .networkSynchronized(WheatVariety.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlourType>> FLOUR_TYPE =
            register("flour_type", builder -> builder
                    .persistent(FlourType.CODEC)
                    .networkSynchronized(FlourType.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<IngredientTypeComponent>> INGREDIENT_TYPE =
            register("ingredient_type", builder -> builder
                    .persistent(IngredientTypeComponent.CODEC)
                    .networkSynchronized(IngredientTypeComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<IngredientCategory>> INGREDIENT_CATEGORY =
            register("ingredient_category", builder -> builder
                    .persistent(IngredientCategory.CODEC)
                    .networkSynchronized(IngredientCategory.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeightComponent>> INGREDIENT_GRAMS =
            register("ingredient_grams", builder -> builder
                    .persistent(WeightComponent.CODEC)
                    .networkSynchronized(WeightComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FoodAdditiveComponent>> FOOD_ADDITIVE =
            register("food_additive", builder -> builder
                    .persistent(FoodAdditiveComponent.CODEC)
                    .networkSynchronized(FoodAdditiveComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DoughRecipeComponent>> DOUGH_RECIPE =
            register("dough_recipe", builder -> builder
                    .persistent(DoughRecipeComponent.CODEC)      // for saving in NBT/JSON
                    .networkSynchronized(DoughRecipeComponent.STREAM_CODEC) // for syncing to clients
            );


//    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BakeryAdditiveType>> BAKERY_ADDITIVE =
//            register("bakery_additive", builder -> builder
//                    .persistent(BakeryAdditiveType.CODEC)
//                    .networkSynchronized(BakeryAdditiveType.STREAM_CODEC)
//            );



    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BakerPctComponent>> BAKER_PERCENTAGES =
            register("baker_percentages", builder -> builder
                    .persistent(BakerPctComponent.CODEC)
                    .networkSynchronized(BakerPctComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BreadType>> BREAD_TYPE =
            register("bread_type", builder -> builder
                    .persistent(BreadType.CODEC)
                    .networkSynchronized(BreadType.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProofingStateComponent>> PROOFING_STATE =
            register("proofing_state", builder -> builder
                    .persistent(ProofingStateComponent.CODEC)
                    .networkSynchronized(ProofingStateComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> DOUGH_PROCESS_TYPE =
            register("dough_process_id", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PanTypeComponent>> PAN_TYPE =
            register("pan_type", builder -> builder
                    .persistent(PanTypeComponent.CODEC)
                    .networkSynchronized(PanTypeComponent.STREAM_CODEC)
            );










    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static ResourceLocation getKey(DataComponentType<?> type) {
        return DATA_COMPONENT_TYPES.getEntries().stream()
                .filter(e -> e.get().equals(type))
                .map(e -> e.getKey().location())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No key found for " + type));
    }



    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}

