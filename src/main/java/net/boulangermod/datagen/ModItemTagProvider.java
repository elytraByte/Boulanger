package net.boulangermod.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;

import net.boulangermod.Boulanger;
import net.boulangermod.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags,
                              String modId) {
        super(output, lookupProvider, blockTags, modId);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(ItemTags.LOGS_THAT_BURN).add(ModBlocks.PINE_LOG.asItem()).add(ModBlocks.PINE_WOOD.asItem()).add(ModBlocks.STRIPPED_PINE_LOG.asItem()).add(ModBlocks.STRIPPED_PINE_WOOD.asItem());

        tag(ItemTags.PLANKS)
                .add(ModBlocks.PINE_PLANKS.asItem());

    }

}
