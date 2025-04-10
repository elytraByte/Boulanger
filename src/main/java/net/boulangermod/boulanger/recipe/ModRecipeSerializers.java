package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    // Suppress the unchecked cast from Registry<RecipeSerializer<?>>
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<RecipeSerializer<RatioRecipe>> SERIALIZERS =
            DeferredRegister.<RecipeSerializer<RatioRecipe>>create(
                    (ResourceKey) Registries.RECIPE_SERIALIZER,  // cast away the wildcard
                    Boulanger.MODID
            );


    public static final DeferredHolder<RecipeSerializer<RatioRecipe>, RatioRecipe.Serializer> RATIO =
            SERIALIZERS.register("ratio",
                    () -> new RatioRecipe.Serializer()
            );



    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }

    public static RecipeSerializer<RatioRecipe> getRatioSerializer() {
        return RATIO.get();
    }
}

