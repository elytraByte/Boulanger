package org.l3e.boulanger.datagen;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.l3e.boulanger.item.FlourItem;

import java.util.HashMap;
import java.util.Map;

public class FlourDataLoader extends SimpleJsonResourceReloadListener {
    public FlourDataLoader() {
        super(new Gson(), "boulanger/flour_data");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectMap,
                         ResourceManager manager,
                         ProfilerFiller profiler) {
        // parse JSON into your FlourData map
    }
}