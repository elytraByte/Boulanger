package net.boulangermod.datagen;


import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.boulangermod.Boulanger;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerator {

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Server-side providers
        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, Boulanger.MODID);

//        event.addProvider(new ModRecipeProvider(packOutput, lookupProvider));

        event.addProvider(new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));

        event.addProvider(blockTagsProvider);
        event.addProvider(new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), Boulanger.MODID));
        event.addProvider(new ModWorldGenProvider(packOutput, lookupProvider));
    }

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();

        // Client-side providers
        event.addProvider(new ModItemModelProvider(packOutput));
        event.addProvider(new ModBlockStateProvider(packOutput));
    }
}