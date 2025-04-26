package net.boulangermod.boulanger.datagen;


import net.boulangermod.boulanger.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.boulangermod.boulanger.Boulanger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Boulanger.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        net.minecraft.data.DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();


        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(),
                new LootTableProvider(
                        packOutput,
                        Collections.emptySet(),
                        List.of(new LootTableProvider.SubProviderEntry(
                                // This lambda should return your loot‐table subprovider:
                                lookup -> new ModBlockLootTableProvider(lookup),
                                LootContextParamSets.BLOCK
                        )),
                        lookupProvider
                )
        );
      BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, Boulanger.MODID, existingFileHelper);generator.addProvider(event.includeServer(), blockTagsProvider);generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));

      generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, Boulanger.MODID, existingFileHelper));
      generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, Boulanger.MODID, existingFileHelper));
      generator.addProvider(event.includeClient(), new ModWorldGenProvider(packOutput, lookupProvider));
    }
}
