package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.data.tags.TagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModPoiTagsProvider extends TagsProvider<PoiType> {
    public ModPoiTagsProvider(PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookupProvider,
                              ExistingFileHelper existingFileHelper) {
        super(output, Registries.POINT_OF_INTEREST_TYPE, lookupProvider, Boulanger.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Generates data/minecraft/tags/point_of_interest_type/acquirable_job_site.json
        // with { "values": [ { "id": "boulanger:baker_poi", "required": false } ] }
        this.tag(PoiTypeTags.ACQUIRABLE_JOB_SITE)
                .addOptional(ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baker_poi"));
    }

    @Override
    public String getName() {
        return "Boulanger POI Tags";
    }
}
