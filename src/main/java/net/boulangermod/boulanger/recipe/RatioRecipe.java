package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
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
    private final double servingWeight;

    public RatioRecipe(ResourceLocation id,
                       List<IngredientComponent> components,
                       double tolerance,
                       ItemStack result,
                       double servingWeight) {
        this.id = Objects.requireNonNull(id);
        this.components = List.copyOf(Objects.requireNonNull(components));
        this.tolerance = tolerance;
        this.result = Objects.requireNonNull(result);
        this.servingWeight = servingWeight;

        // ---------- Advanced Logging ----------
        LOG.info("🔧 Loaded RatioRecipe: {}", id);
        LOG.info("   → Result: {} ({}g)", result.getItem(), servingWeight);
        LOG.info("   → Tolerance: {} ({}%)", tolerance, tolerance * 100.0);

        if (tolerance > 1.0) {
            LOG.warn("⚠ Recipe {} has an unusually high tolerance (>100%): {}!", id, tolerance);
        } else if (tolerance > 0.5) {
            LOG.warn("⚠ Recipe {} has a high tolerance: {} ({}%)", id, tolerance, tolerance * 100.0);
        }

        for (IngredientComponent comp : components) {
            String allowed = comp.allowedItems().isEmpty()
                    ? "any"
                    : comp.allowedItems().toString();
            LOG.info("   → {}: {}% (allowed: {})",
                    comp.category(), comp.targetPercent(), allowed);
        }
        LOG.info("----------------------------------------");
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

    public List<IngredientComponent> getComponents() {
        return components;
    }

    public double getTolerance() {
        return tolerance;
    }

    public double getServingWeight() {
        return servingWeight;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(MixingContainer inv) {
        return Recipe.super.getRemainingItems(inv);
    }

    public IngredientRequirement[] getItemRequirements() {
        return new IngredientRequirement[0];
    }

    public boolean hasOnlyOneFlour() {
        return components.stream()
                .filter(c -> c.category() == IngredientCategory.FLOUR)
                .count() == 1;
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
                        .forGetter(r -> r.result),

                Codec.DOUBLE
                        .fieldOf("serving_weight")
                        .forGetter(RatioRecipe::getServingWeight)

        ).apply(inst, RatioRecipe::new));

        @Override
        public MapCodec<RatioRecipe> codec() {
            return CODEC;
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC,           RatioRecipe::getId,
                        StreamCodecsCompat.list(IngredientComponent.STREAM_CODEC), RatioRecipe::getComponents,
                        StreamCodecsCompat.DOUBLE,               RatioRecipe::getTolerance,
                        ItemStack.STREAM_CODEC,                  r -> r.result,
                        StreamCodecsCompat.DOUBLE,               RatioRecipe::getServingWeight,
                        RatioRecipe::new
                );

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> streamCodec() {
            return STREAM_CODEC;
        }

    }
}
