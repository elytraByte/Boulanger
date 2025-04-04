package net.boulangermod.datagen;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;


public class FlourDataLoader extends SimpleJsonResourceReloadListener {


    protected FlourDataLoader(HolderLookup.Provider provider, Codec codec, ResourceKey registryKey) {
        super(provider, codec, registryKey);
    }

    @Override
    protected void apply(Object object, ResourceManager resourceManager, ProfilerFiller profiler) {

    }
}