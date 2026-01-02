package net.boulangermod.boulanger.recipe.serializer;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class DoughProcessRecipeSerializer implements RecipeSerializer<DoughProcessRecipe> {

    @Override
    public MapCodec<DoughProcessRecipe> codec() {
        return DoughProcessRecipe.MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> streamCodec() {
        return DoughProcessRecipe.STREAM_CODEC.cast();
    }
}
