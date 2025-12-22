package net.boulangermod.boulanger.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.boulangermod.boulanger.Boulanger.MODID;

public final class ModRecipeTypes {
    private ModRecipeTypes() {}

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<DoughProcessRecipe>> DOUGH_PROCESS =
            TYPES.register("dough_process",
                    () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "dough_process")));
}