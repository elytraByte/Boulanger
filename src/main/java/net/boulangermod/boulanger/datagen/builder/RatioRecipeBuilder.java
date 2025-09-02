package net.boulangermod.boulanger.datagen.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.boulangermod.boulanger.recipe.IngredientRequirement;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.recipe.RatioRecipe.Serializer;
import net.boulangermod.boulanger.recipe.IngredientComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Datagen builder for {@link RatioRecipe}.
 *
 * Writes optional {@code roll_size_g} / {@code loaf_size_g}.
 * The old {@link #servingWeight(double)} is kept for source compatibility
 * and now maps to {@code loaf_size_g}.
 */
public class RatioRecipeBuilder {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ResourceLocation id;
    private final ItemStack result;
    private final double tolerance;
    private final List<IngredientComponent> components = new ArrayList<>();
    private final List<IngredientRequirement> itemRequirements = new ArrayList<>();

    // New optional unit sizes (omitted from JSON when null)
    private @Nullable Integer rollSizeG = null;
    private @Nullable Integer loafSizeG = null;

    // Back-compat only: if used and loafSizeG not set, we map it to loaf_size_g.
    @Deprecated
    private @Nullable Double legacyServingWeightG = null;

    public RatioRecipeBuilder(ResourceLocation id, ItemStack result, double tolerance) {
        this.id = Objects.requireNonNull(id);
        this.result = Objects.requireNonNull(result);
        this.tolerance = tolerance;
    }

    public RatioRecipeBuilder addComponent(
            IngredientCategory category,
            double percent,
            List<ResourceLocation> allowedItems
    ) {
        components.add(new IngredientComponent(category, percent, allowedItems));
        return this;
    }

    public RatioRecipeBuilder addItemRequirement(ResourceLocation itemId, double amount) {
        this.itemRequirements.add(new IngredientRequirement(itemId, amount));
        return this;
    }

    /** Prefer {@link #loafSizeG(int)} or {@link #rollSizeG(int)}. */
    @Deprecated
    public RatioRecipeBuilder servingWeight(double grams) {
        this.legacyServingWeightG = grams;
        return this;
    }

    /** Sets the per-serving size for a roll-shaped unit (grams). */
    public RatioRecipeBuilder rollSizeG(int grams) {
        this.rollSizeG = grams;
        return this;
    }

    /** Sets the per-serving size for a loaf-shaped unit (grams). */
    public RatioRecipeBuilder loafSizeG(int grams) {
        this.loafSizeG = grams;
        return this;
    }

    public void save(RecipeOutput out) {
        JsonObject json = new JsonObject();

        // required fields
        json.addProperty("id", id.toString());
        json.addProperty("tolerance", this.tolerance);

        // components (encode via the IngredientComponent CODEC so keys match the runtime)
        JsonElement compsEl = IngredientComponent.CODEC
                .listOf()
                .encodeStart(JsonOps.INSTANCE, this.components)
                .getOrThrow();
        json.add("components", compsEl);

        // result (simple item/count form)
        JsonObject resultObj = new JsonObject();
        Item item = result.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        resultObj.addProperty("item", itemId.toString());
        if (this.result.getCount() != 1) {
            resultObj.addProperty("count", this.result.getCount());
        }
        json.add("result", resultObj);

        // new optional sizes
        Integer loafToWrite = this.loafSizeG;
        if (loafToWrite == null && legacyServingWeightG != null) {
            loafToWrite = (int) Math.round(legacyServingWeightG);
        }
        if (this.rollSizeG != null) {
            json.addProperty("roll_size_g", this.rollSizeG);
        }
        if (loafToWrite != null) {
            json.addProperty("loaf_size_g", loafToWrite);
        }

        // result — let vanilla encode it so field names match the runtime (“id”, “count”, …)
        JsonElement resultEl = ItemStack.CODEC
                .encodeStart(JsonOps.INSTANCE, this.result)
                .getOrThrow();
        json.add("result", resultEl);


        LOGGER.debug("Generated recipe JSON for {}: {}", id, json);

        Codec<RatioRecipe> codec = Serializer.CODEC.codec();
        RatioRecipe recipe = codec.parse(JsonOps.INSTANCE, json)
                .getOrThrow();

        out.accept(id, recipe, null);
    }
}
