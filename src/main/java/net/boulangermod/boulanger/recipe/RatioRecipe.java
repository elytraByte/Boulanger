package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.IngredientStack;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.boulangermod.boulanger.recipe.ModRecipeSerializers.*;

public class RatioRecipe implements Recipe<MixingContainer> {
    private static final Logger LOG = LogManager.getLogger(RatioRecipe.class);

    private final ResourceLocation id;
    private final Map<IngredientCategory, Double> targets;
    private final double tolerance;
    private final ItemStack output;
    private final List<IngredientRequirement> itemRequirements;
    private final Map<IngredientCategory, List<ResourceLocation>> allowedItems;

    public RatioRecipe(ResourceLocation id,
                       Map<IngredientCategory, Double> targets,
                       double tolerance,
                       ItemStack output,
                       List<IngredientRequirement> itemRequirements,
                       @Nullable Map<IngredientCategory, List<ResourceLocation>> allowedItems) {
        this.id = Objects.requireNonNull(id);
        this.targets = Objects.requireNonNull(targets);
        this.tolerance = tolerance;
        this.output = Objects.requireNonNull(output);
        this.itemRequirements = Objects.requireNonNull(itemRequirements);
        this.allowedItems = (allowedItems != null ? allowedItems : Map.of());
    }

    @Override
    public boolean matches(MixingContainer mixingContainer, Level level) {
        throw new UnsupportedOperationException("Use matches(Map<IngredientCategory, Double>)");
    }

    @Override
    public ItemStack assemble(MixingContainer inv, HolderLookup.Provider ctx) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider ctx) {
        return output.copy();
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

    @Override
    public NonNullList<ItemStack> getRemainingItems(MixingContainer inv) {
        NonNullList<ItemStack> rem = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < rem.size(); i++) {
            ItemStack in = inv.getItem(i);
            if (!in.isEmpty()) {
                rem.set(i, in.getItem().getCraftingRemainingItem(in));
            }
        }
        return rem;
    }

    public boolean matches(Map<IngredientCategory, Double> actualPct) {
        Double flourPct = actualPct.get(IngredientCategory.FLOUR);
        if (flourPct == null || flourPct == 0.0) return false;
        for (var entry : targets.entrySet()) {
            IngredientCategory cat = entry.getKey();
            double target = entry.getValue();
            double actual = actualPct.getOrDefault(cat, 0.0);
            if (Math.abs(actual - target) > tolerance) {
                return false;
            }
        }
        return true;
    }

    public boolean checkAllowedItems(List<IngredientStack> ingredients) {
        for (IngredientStack st : ingredients) {
            IngredientCategory cat = st.getCategory();
            List<ResourceLocation> allowed = allowedItems.get(cat);

            if (allowed != null && !allowed.isEmpty()) {
                ResourceLocation actualId;
                if (st.getFlourType() != null) {
                    actualId = ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, st.getFlourType().getId());
                } else {
                    actualId = BuiltInRegistries.ITEM.getKey(st.getActualItem());
                }

                LOG.debug("Whitelist check for {} → actualId = {}", cat, actualId);
                if (!allowed.contains(actualId)) {
                    LOG.warn("Ingredient {} not in allowed list {}", actualId, allowed);
                    return false;
                }
            }
        }
        return true;
    }

    public Map<IngredientCategory, Double> getTargets() { return targets; }
    public double getTolerance() { return tolerance; }
    public ItemStack getOutput() { return output; }
    public List<IngredientRequirement> getItemRequirements() { return itemRequirements; }
    public Map<IngredientCategory, List<ResourceLocation>> getAllowedItems() { return allowedItems; }

    public static final class Serializer implements RecipeSerializer<RatioRecipe> {
        public static final MapCodec<RatioRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(RatioRecipe::getId),
                StreamCodecsCompat.mapCodec(IngredientCategory.CODEC, Codec.DOUBLE).fieldOf("targets").forGetter(RatioRecipe::getTargets),
                Codec.DOUBLE.fieldOf("tolerance").forGetter(RatioRecipe::getTolerance),
                ItemStack.CODEC.fieldOf("result").forGetter(RatioRecipe::getOutput),
                Codec.list(IngredientRequirement.CODEC.codec()).optionalFieldOf("requirements", List.of()).forGetter(RatioRecipe::getItemRequirements),
                StreamCodecsCompat.mapCodec(IngredientCategory.CODEC, Codec.list(ResourceLocation.CODEC)).optionalFieldOf("allowed_items", Map.of()).forGetter(RatioRecipe::getAllowedItems)
        ).apply(inst, RatioRecipe::new));

        @Override public MapCodec<RatioRecipe> codec() { return CODEC; }

        public static final StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, recipe -> recipe.id,
                StreamCodecsCompat.map(IngredientCategory.STREAM_CODEC, StreamCodecsCompat.DOUBLE), RatioRecipe::getTargets,
                StreamCodecsCompat.DOUBLE, RatioRecipe::getTolerance,
                ItemStack.STREAM_CODEC, RatioRecipe::getOutput,
                StreamCodecsCompat.list(IngredientRequirement.STREAM_CODEC), RatioRecipe::getItemRequirements,
                StreamCodecsCompat.map(IngredientCategory.STREAM_CODEC, StreamCodecsCompat.list(ResourceLocation.STREAM_CODEC)), RatioRecipe::getAllowedItems,
                (id, t, tol, out, reqs, allowed) -> new RatioRecipe(id, t, tol, out, reqs, allowed)
        );

        @Override public StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
