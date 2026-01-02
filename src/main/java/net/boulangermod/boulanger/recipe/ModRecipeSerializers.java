package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.boulangermod.boulanger.recipe.serializer.DoughProcessRecipeSerializer;

public final class ModRecipeSerializers {
    private ModRecipeSerializers() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Boulanger.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RatioRecipe>> RATIO_SERIALIZER =
            SERIALIZERS.register("ratio", RatioRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DoughProcessRecipe>> DOUGH_PROCESS =
            SERIALIZERS.register("dough_process", DoughProcessRecipeSerializer::new);

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}

