package org.l3e.boulanger.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.block.ModBlocks;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Boulanger.MODID, existingFileHelper);
    }



    @Override
    protected void addTags(HolderLookup.Provider provider) {

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.MIXER.get())
                .add(ModBlocks.WOOD_GASIFIER.get());

        this.tag(BlockTags.LOGS_THAT_BURN).add(ModBlocks.PINE_LOG.get()).add(ModBlocks.PINE_WOOD.get()).add(ModBlocks.STRIPPED_PINE_LOG.get()).add(ModBlocks.STRIPPED_PINE_WOOD.get());

    }
}
