package net.boulangermod.boulanger.datagen.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static net.boulangermod.boulanger.recipe.DoughProcessRecipe.Serializer.CODEC;

public class DoughProcessRecipeBuilder {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ResourceLocation id;
    private final ResourceLocation doughType;
    private final List<ProcessingStep> steps = new ArrayList<>();

    public DoughProcessRecipeBuilder(ResourceLocation id, ResourceLocation doughType) {
        this.id = Objects.requireNonNull(id);
        this.doughType = Objects.requireNonNull(doughType);
    }

    public DoughProcessRecipeBuilder addStep(StepType type, int durationTicks) {
        this.steps.add(new ProcessingStep(type, durationTicks));
        return this;
    }

    public DoughProcessRecipeBuilder addStep(StepType type) {
        return addStep(type, 0);
    }

    public void save(RecipeOutput output) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "boulanger:dough_process");
        json.addProperty("id", id.toString());
        json.addProperty("dough_type", doughType.toString());

        JsonArray stepArray = new JsonArray();
        for (ProcessingStep step : steps) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", step.type().name());
            if (step.durationTicks() > 0) {
                obj.addProperty("duration", step.durationTicks());
            }
            stepArray.add(obj);
        }

        json.add("steps", stepArray);

        LOGGER.debug("Generated dough process recipe JSON for {}: {}", id, json);

        // Use codec to convert the JsonObject into a DoughProcessRecipe instance
        DataResult<DoughProcessRecipe> parsed = CODEC.codec().parse(JsonOps.INSTANCE, json);
        DoughProcessRecipe recipe = parsed.getOrThrow();


        output.accept(id, recipe, null);
    }
}
