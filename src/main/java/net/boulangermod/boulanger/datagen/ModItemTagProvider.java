package net.boulangermod.boulanger.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider,
                              CompletableFuture<TagLookup<Block>> pBlockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, pLookupProvider, pBlockTags, Boulanger.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(ItemTags.LOGS_THAT_BURN).add(ModBlocks.PINE_LOG.asItem()).add(ModBlocks.PINE_WOOD.asItem()).add(ModBlocks.STRIPPED_PINE_LOG.asItem()).add(ModBlocks.STRIPPED_PINE_WOOD.asItem());

        tag(ItemTags.PLANKS)
                .add(ModBlocks.PINE_PLANKS.asItem());

    }
}
