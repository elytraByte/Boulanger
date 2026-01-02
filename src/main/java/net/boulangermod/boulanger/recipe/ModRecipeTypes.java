package net.boulangermod.boulanger.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.boulangermod.boulanger.Boulanger.MOD_ID;

public final class ModRecipeTypes {
    private ModRecipeTypes() {}

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RatioRecipe>> RATIO =
            TYPES.register("ratio",
                    () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MOD_ID, "ratio")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<DoughProcessRecipe>> DOUGH_PROCESS =
            TYPES.register("dough_process",
                    () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MOD_ID, "dough_process")));


    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
