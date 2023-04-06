package org.l3e.Boulanger.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.l3e.Boulanger.Boulanger;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Boulanger.MOD_ID);

    public static final RegistryObject<RecipeSerializer<StoneMillRecipe>> STONE_MILL_SERIALIZER =
        SERIALIZERS.register("stone_milling", () -> StoneMillRecipe.Serializer.INSTANCE);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
