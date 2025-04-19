package net.boulangermod.boulanger.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * A recipe matching baker's percentages by category and optionally by specific ingredient items.
 */
public class RatioRecipe implements Recipe<MixingContainer> {
    public static final RecipeType<RatioRecipe> TYPE =
            RecipeType.register("boulanger:ratio");

    private final ResourceLocation id;
    private final Map<IngredientCategory, Double> targets;
    private final List<IngredientRequirement> itemRequirements;
    private final double tolerance;
    private final ItemStack output;

    /**
     * Main constructor including optional item-specific requirements.
     */
    public RatioRecipe(ResourceLocation id,
                       Map<IngredientCategory, Double> targets,
                       double tolerance,
                       ItemStack output,
                       List<IngredientRequirement> itemRequirements) {
        this.id = id;
        this.targets = targets;
        this.tolerance = tolerance;
        this.output = output.copy();
        this.itemRequirements = List.copyOf(itemRequirements);
    }

    /**
     * Backwards-compatible constructor with no item-specific requirements.
     */
    public RatioRecipe(ResourceLocation id,
                       Map<IngredientCategory, Double> targets,
                       double tolerance,
                       ItemStack output) {
        this(id, targets, tolerance, output, List.of());
    }

    public ResourceLocation getId() { return id; }
    public Map<IngredientCategory, Double> getTargets() { return targets; }
    public double getTolerance() { return tolerance; }
    public List<IngredientRequirement> getItemRequirements() { return itemRequirements; }

    @Override public RecipeType<?> getType() { return TYPE; }

    @Override
    public boolean matches(MixingContainer inv, Level level) {
        // TODO: implement percent+item matching logic here
        return true;
    }

    @Override
    public ItemStack assemble(MixingContainer inv, HolderLookup.Provider ctx) {
        return output.copy();
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider access) { return output.copy(); }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.getRatioSerializer(); }

    // === IngredientRequirement ===
    /**
     * A specific ingredient (by ID), its category, and baker's percentage.
     */
    public static record IngredientRequirement(
            ResourceLocation ingredientId,
            IngredientCategory category,
            double percent
    ) {
        public static final MapCodec<IngredientRequirement> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("ingredient").forGetter(IngredientRequirement::ingredientId),
                IngredientCategory.CODEC.fieldOf("category").forGetter(IngredientRequirement::category),
                Codec.DOUBLE.fieldOf("percent").forGetter(IngredientRequirement::percent)
        ).apply(inst, IngredientRequirement::new));
    }

    // === Serializer ===
    public static final class Serializer implements RecipeSerializer<RatioRecipe> {
        private static final MapCodec<RatioRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(RatioRecipe::getId),
                StreamCodecsCompat.mapCodec(IngredientCategory.CODEC, Codec.DOUBLE)
                        .fieldOf("targets").forGetter(RatioRecipe::getTargets),
                Codec.DOUBLE.fieldOf("tolerance").forGetter(RatioRecipe::getTolerance),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output),
                IngredientRequirement.CODEC.codec().listOf()
                        .optionalFieldOf("requirements", List.of())
                        .forGetter(RatioRecipe::getItemRequirements)
        ).apply(inst, RatioRecipe::new));

        @Override public MapCodec<RatioRecipe> codec() { return CODEC; }

        /** JSON → recipe */
        public static RatioRecipe fromJson(ResourceLocation id, JsonObject json) {
            return CODEC.codec()
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                    .getOrThrow();
        }

        /** Network codec backed by JSON string. */
        private static final StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> STREAM_CODEC =
                new StreamCodec<RegistryFriendlyByteBuf, RatioRecipe>() {
                    @Override
                    public RatioRecipe decode(RegistryFriendlyByteBuf buf) {
                        ResourceLocation rid = buf.readResourceLocation();
                        JsonObject obj = net.minecraft.util.GsonHelper.parse(buf.readUtf(Short.MAX_VALUE));
                        return fromJson(rid, obj);
                    }
                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, RatioRecipe recipe) {
                        buf.writeResourceLocation(recipe.getId());
                        var je = CODEC.codec()
                                .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, recipe)
                                .getOrThrow();
                        buf.writeUtf(je.toString(), Short.MAX_VALUE);
                    }
                };

        @Override public StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
