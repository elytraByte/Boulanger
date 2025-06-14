package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {

    // 1️⃣ Serializers
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Boulanger.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RatioRecipe>> RATIO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ratio", RatioRecipe.Serializer::new);


    // 2️⃣ Types
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Boulanger.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RatioRecipe>> RATIO_TYPE =
            RECIPE_TYPES.register("ratio", RecipeType::simple);




    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DoughProcessRecipe>> DOUGH_PROCESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("dough_process", DoughProcessRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<DoughProcessRecipe>> DOUGH_PROCESS_TYPE =
            RECIPE_TYPES.register("dough_process", () -> new RecipeType<>() {});


    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}