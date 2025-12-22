package net.boulangermod.boulanger.recipe.serializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.item.PortionKind;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.PanServing;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.mojang.serialization.MapCodec;

public class DoughProcessRecipeSerializer implements RecipeSerializer<DoughProcessRecipe> {
    public static final DoughProcessRecipeSerializer INSTANCE = new DoughProcessRecipeSerializer();

    @Override
    public MapCodec<DoughProcessRecipe> codec() {
        return DoughProcessRecipe.MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> streamCodec() {
        // DoughProcessRecipe.STREAM_CODEC; // if yours is ByteBuf-typed, use .cast()
        return DoughProcessRecipe.STREAM_CODEC.cast();
    }
}
