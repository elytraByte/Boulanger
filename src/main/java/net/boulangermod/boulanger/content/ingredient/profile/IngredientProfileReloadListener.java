package net.boulangermod.boulanger.content.ingredient.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class IngredientProfileReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public IngredientProfileReloadListener() {
        super(GSON, "ingredient_profiles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, IngredientProfile> out = new HashMap<>();

        for (var entry : jsons.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            var parsed = IngredientProfile.CODEC.parse(JsonOps.INSTANCE, json);
            parsed.resultOrPartial(msg ->
                    LOG.error("IngredientProfile parse error in {}: {}", fileId, msg)
            ).ifPresent(profile -> {
                IngredientProfile prev = out.put(profile.id(), profile);
                if (prev != null) {
                    LOG.warn("Duplicate IngredientProfile id {} (file {}). Overwriting.", profile.id(), fileId);
                }
            });
        }

        IngredientProfiles.replaceAll(out);
    }
}
