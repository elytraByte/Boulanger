package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.recipe.serializer.DoughProcessRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    private ModRecipeSerializers() {}

    // --- Serializers ---
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Boulanger.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RatioRecipe>> RATIO_SERIALIZER =
            SERIALIZERS.register("ratio", RatioRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DoughProcessRecipe>> DOUGH_PROCESS =
            SERIALIZERS.register("dough_process", () -> DoughProcessRecipeSerializer.INSTANCE);

    // --- Types ---
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Boulanger.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RatioRecipe>> RATIO_TYPE =
            TYPES.register("ratio", RecipeType::simple);

    public static final DeferredHolder<RecipeType<?>, RecipeType<DoughProcessRecipe>> DOUGH_PROCESS_TYPE =
             TYPES.register("dough_process", RecipeType::simple);

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
        TYPES.register(bus);
    }
}