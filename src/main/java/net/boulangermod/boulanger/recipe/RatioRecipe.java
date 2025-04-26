package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;

import static net.boulangermod.boulanger.recipe.ModRecipeSerializers.*;

public class RatioRecipe implements Recipe<MixingContainer> {
    private static final Logger LOG = LogManager.getLogger();

    private final ResourceLocation id;
    private final List<IngredientComponent> components;
    private final double tolerance;
    private final ItemStack result;

    public RatioRecipe(ResourceLocation id,
                       List<IngredientComponent> components,
                       double tolerance,
                       ItemStack result) {
        this.id         = Objects.requireNonNull(id);
        this.components = List.copyOf(Objects.requireNonNull(components));
        this.tolerance  = tolerance;
        this.result     = Objects.requireNonNull(result);
    }

    /** We match in MixingBlockEntity; this stays unimplemented. */
    @Override
    public boolean matches(MixingContainer inv, Level level) {
        throw new UnsupportedOperationException("Use MixingBlockEntity.findMatchingRecipe()");
    }

    @Override
    public ItemStack assemble(MixingContainer inv, HolderLookup.Provider ctx) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider ctx) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RATIO_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RATIO_TYPE.get();
    }

    public ResourceLocation getId() {
        return id;
    }

    /** Exposed so your mixer can loop over each slot‐component. */
    public List<IngredientComponent> getComponents() {
        return components;
    }

    public double getTolerance() {
        return tolerance;
    }

    /** We don’t track per‐item requirements in this design. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(MixingContainer inv) {
        return Recipe.super.getRemainingItems(inv);
    }

    public IngredientRequirement[] getItemRequirements() {
        return new IngredientRequirement[0];
    }

    // -------------------------------------------------------------
    // SERIALIZER
    // -------------------------------------------------------------
    public static final class Serializer implements RecipeSerializer<RatioRecipe> {
        public static final MapCodec<RatioRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ResourceLocation.CODEC
                        .fieldOf("id")
                        .forGetter(RatioRecipe::getId),

                IngredientComponent.CODEC
                        .listOf()
                        .fieldOf("components")
                        .forGetter(RatioRecipe::getComponents),

                Codec.DOUBLE
                        .fieldOf("tolerance")
                        .forGetter(RatioRecipe::getTolerance),

                ItemStack.CODEC
                        .fieldOf("result")
                        .forGetter(r -> r.result)

        ).apply(inst, RatioRecipe::new));

        @Override
        public MapCodec<RatioRecipe> codec() {
            return CODEC;
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC,            RatioRecipe::getId,
                        StreamCodecsCompat.list(IngredientComponent.STREAM_CODEC),
                        RatioRecipe::getComponents,
                        StreamCodecsCompat.DOUBLE,               RatioRecipe::getTolerance,
                        ItemStack.STREAM_CODEC,                  r -> r.result,
                        RatioRecipe::new
                );

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
