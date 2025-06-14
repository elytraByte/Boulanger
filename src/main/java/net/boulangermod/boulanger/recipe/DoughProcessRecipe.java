package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

import static net.boulangermod.boulanger.recipe.ModRecipeSerializers.*;

public class DoughProcessRecipe implements Recipe<DoughProcessInput> {
    private final ResourceLocation id;
    private final ResourceLocation doughType;
    private final List<ProcessingStep> steps;

    public DoughProcessRecipe(ResourceLocation id, ResourceLocation doughType, List<ProcessingStep> steps) {
        this.id = Objects.requireNonNull(id);
        this.doughType = Objects.requireNonNull(doughType);
        this.steps = List.copyOf(Objects.requireNonNull(steps));
    }



    public ResourceLocation getDoughType() {
        return doughType;
    }

    public List<ProcessingStep> getSteps() {
        return steps;
    }

        public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean matches(DoughProcessInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(DoughProcessInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider ctx) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return DOUGH_PROCESS_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return DOUGH_PROCESS_TYPE.get(); }

    // ------------------------------------------------------------------
    // SERIALIZER
    // ------------------------------------------------------------------
    public static final class Serializer implements RecipeSerializer<DoughProcessRecipe> {
        public static final MapCodec<DoughProcessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(DoughProcessRecipe::getId),
                ResourceLocation.CODEC.fieldOf("dough_type").forGetter(DoughProcessRecipe::getDoughType),
                ProcessingStep.CODEC.listOf().fieldOf("steps").forGetter(DoughProcessRecipe::getSteps)
        ).apply(instance, DoughProcessRecipe::new));

        @Override public MapCodec<DoughProcessRecipe> codec() { return CODEC; }

        public static final StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, DoughProcessRecipe::getId,
                        ResourceLocation.STREAM_CODEC, DoughProcessRecipe::getDoughType,
                        StreamCodecsCompat.list(ProcessingStep.STREAM_CODEC), DoughProcessRecipe::getSteps,
                        DoughProcessRecipe::new
                );


        @Override public StreamCodec<RegistryFriendlyByteBuf, DoughProcessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

}
