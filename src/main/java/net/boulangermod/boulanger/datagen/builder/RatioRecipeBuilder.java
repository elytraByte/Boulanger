package net.boulangermod.boulanger.datagen.builder;

import com.google.gson.JsonArray;
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
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RatioRecipeBuilder {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ResourceLocation id;
    private final ItemStack result;
    private final double tolerance;
    private final List<IngredientComponent> components = new ArrayList<>();
    private final List<IngredientRequirement> itemRequirements = new ArrayList<>();
    private double servingWeight = 454.0; // default: 1 lb

    public RatioRecipeBuilder(ResourceLocation id, ItemStack result, double tolerance) {
        this.id = id;
        this.result = result;
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

    public RatioRecipeBuilder servingWeight(double grams) {
        this.servingWeight = grams;
        return this;
    }

    public void save(RecipeOutput out) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "boulanger:ratio");
        json.addProperty("id", id.toString());

        JsonObject res = new JsonObject();
        res.addProperty("id", BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
        res.addProperty("count", result.getCount());
        json.add("result", res);

        JsonArray compArr = new JsonArray();
        for (var c : components) {
            JsonObject obj = new JsonObject();
            obj.addProperty("category", c.category().name());
            obj.addProperty("target_percent", c.targetPercent());
            JsonArray allow = new JsonArray();
            c.allowedItems().forEach(rl -> allow.add(rl.toString()));
            obj.add("allowed_items", allow);
            compArr.add(obj);
        }
        json.add("components", compArr);

        json.addProperty("tolerance", tolerance);
        json.addProperty("serving_weight", servingWeight);

        if (!itemRequirements.isEmpty()) {
            JsonArray reqArr = new JsonArray();
            for (IngredientRequirement req : itemRequirements) {
                JsonObject o = new JsonObject();
                o.addProperty("itemId", req.getItemId().toString());
                o.addProperty("amount", req.getAmount());
                reqArr.add(o);
            }
            json.add("requirements", reqArr);
        }

        LOGGER.debug("Generated recipe JSON for {}: {}", id, json);

        Codec<RatioRecipe> codec = Serializer.CODEC.codec();
        DataResult<RatioRecipe> parsed = codec.parse(JsonOps.INSTANCE, json);
        RatioRecipe recipe = parsed.getOrThrow();

        out.accept(id, recipe, null);
    }
}
