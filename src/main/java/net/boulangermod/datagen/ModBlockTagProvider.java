package net.boulangermod.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;

import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.boulangermod.block.ModBlocks;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider implements DataProvider {
    public ModBlockTagProvider(PackOutput output,
                               CompletableFuture<HolderLookup.Provider> lookupProvider,
                               String modId) {
        super(output, lookupProvider, modId);
                }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

//        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
//               .add(ModBlocks.WOOD_GASIFIER.get());

        this.tag(BlockTags.LOGS_THAT_BURN).add(ModBlocks.PINE_LOG.get()).add(ModBlocks.PINE_WOOD.get()).add(ModBlocks.STRIPPED_PINE_LOG.get()).add(ModBlocks.STRIPPED_PINE_WOOD.get());

        this.tag(BlockTags.LOGS_THAT_BURN)
                .add(ModBlocks.PINE_LOG.get())
                .add(ModBlocks.PINE_WOOD.get())
                .add(ModBlocks.STRIPPED_PINE_LOG.get())
                .add(ModBlocks.STRIPPED_PINE_WOOD.get());

    }
}



