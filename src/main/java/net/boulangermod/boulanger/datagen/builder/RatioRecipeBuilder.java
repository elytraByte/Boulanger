package net.boulangermod.boulanger.datagen.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
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
    private final ItemStack       result;
    private final double          tolerance;
    private final List<IngredientComponent> components = new ArrayList<>();

    public RatioRecipeBuilder(ResourceLocation id,
                              ItemStack result,
                              double tolerance) {
        this.id        = id;
        this.result    = result;
        this.tolerance = tolerance;
    }

    public RatioRecipeBuilder addComponent(
            IngredientCategory category,
            double             percent,
            List<ResourceLocation> allowedItems
    ) {
        components.add(new IngredientComponent(category, percent, allowedItems));
        return this;
    }

    /**
     * 1) Build the JSON exactly as before
     * 2) Log it (so you can eyeball it)
     * 3) Parse it into a RatioRecipe via your Codec
     * 4) Hand that recipe to the provider for actual file‐generation
     */
    public void save(RecipeOutput out) {
        // 1) build the JSON
        JsonObject json = new JsonObject();
        json.addProperty("type",      "boulanger:ratio");
        json.addProperty("id",        id.toString());

        // result
        JsonObject res = new JsonObject();
        res.addProperty("id",
                BuiltInRegistries.ITEM.getKey(result.getItem()).toString()
        );
        res.addProperty("count", result.getCount());
        json.add("result", res);

        // components
        JsonArray compArr = new JsonArray();
        for (var c : components) {
            JsonObject obj = new JsonObject();
            obj.addProperty("category",       c.category().name());
            obj.addProperty("target_percent", c.targetPercent());
            JsonArray allow = new JsonArray();
            c.allowedItems().forEach(rl -> allow.add(rl.toString()));
            obj.add("allowed_items", allow);
            compArr.add(obj);
        }
        json.add("components", compArr);

        // tolerance
        json.addProperty("tolerance", tolerance);

        // 2) log it
        LOGGER.debug("Generated recipe JSON for {}: {}", id, json);

        // 3) parse it via your Codec to catch any errors early
        Codec<RatioRecipe> codec = Serializer.CODEC.codec();
        DataResult<RatioRecipe> parsed = codec.parse(JsonOps.INSTANCE, json);
        RatioRecipe recipe = parsed.getOrThrow(
        );



        // 4) hand it off (no advancement, no conditions)
        out.accept(id, recipe, null);
    }
}
