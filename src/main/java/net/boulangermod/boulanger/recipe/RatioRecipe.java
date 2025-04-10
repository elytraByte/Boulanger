package net.boulangermod.boulanger.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RatioRecipe implements Recipe<MixingContainer> {
    public static final RecipeType<RatioRecipe> TYPE =
            RecipeType.register("boulanger:ratio");

    private final ResourceLocation id;
    private final Map<IngredientCategory, Double> targets;
    private final double tolerance;
    private final ItemStack output;

    public RatioRecipe(ResourceLocation id,
                       Map<IngredientCategory, Double> targets,
                       double tolerance,
                       ItemStack output) {
        this.id = id;
        this.targets = targets;
        this.tolerance = tolerance;
        this.output = output;
    }

    public ResourceLocation getId() { return id; }
    public Map<IngredientCategory, Double> getTargets() { return targets; }
    public double getTolerance() { return tolerance; }

    @Override
    public boolean matches(MixingContainer inv, Level level) {
        return true;
    }

    @Override
    public ItemStack assemble(MixingContainer inv, HolderLookup.Provider ctx) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider access) {
        return output.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.getRatioSerializer();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return TYPE;
    }

    // === Serializer inner class ===

    public static final class Serializer implements RecipeSerializer<RatioRecipe> {
        // 1) JSON‐backed codec
        private static final MapCodec<RatioRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(RatioRecipe::getId),
                StreamCodecsCompat.mapCodec(IngredientCategory.CODEC, Codec.DOUBLE)
                        .fieldOf("targets").forGetter(RatioRecipe::getTargets),
                Codec.DOUBLE.fieldOf("tolerance").forGetter(RatioRecipe::getTolerance),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output)
        ).apply(inst, RatioRecipe::new));

        @Override
        public MapCodec<RatioRecipe> codec() {
            return CODEC;
        }

        //@Override
        public Codec<RatioRecipe> codec(ResourceLocation recipeId) {
            // NeoForge 1.21 uses this signature for JSON loading
            return CODEC.codec();
        }

        // 2) Network‐sync codec, implemented as an anonymous class to avoid lambda‐type issues
        private static final StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> STREAM_CODEC =
                new StreamCodec<RegistryFriendlyByteBuf, RatioRecipe>() {
                    @Override
                    public RatioRecipe decode(RegistryFriendlyByteBuf buf) {
                        ResourceLocation id = buf.readResourceLocation();
                        String json = buf.readUtf(Short.MAX_VALUE);
                        JsonObject obj = net.minecraft.util.GsonHelper.parse(json);
                        return fromJson(id, obj);
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, RatioRecipe recipe) {
                        buf.writeResourceLocation(recipe.getId());
                        // encode to JSON string
                        com.google.gson.JsonElement je = CODEC.codec()
                                .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, recipe)
                                .getOrThrow();
                        buf.writeUtf(je.toString(), Short.MAX_VALUE);
                    }
                };

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RatioRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        /** Called reflectively by the recipe manager when loading JSON. */
        public static RatioRecipe fromJson(ResourceLocation id, JsonObject json) {
            return CODEC.codec()
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                    .getOrThrow();
        }

        /** Called reflectively by the recipe manager when reading from network. */
        public RatioRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            // NeoForge passes a FriendlyByteBuf that's actually a RegistryFriendlyByteBuf
            return STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        }

        /** Called reflectively by the recipe manager when writing to network. */
        public void toNetwork(FriendlyByteBuf buf, RatioRecipe recipe) {
            STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, recipe);
        }
    }

}
