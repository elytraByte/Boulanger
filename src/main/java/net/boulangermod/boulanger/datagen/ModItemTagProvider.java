//package net.boulangermod.boulanger.datagen;
//
//import net.boulangermod.boulanger.Boulanger;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.data.PackOutput;
//import net.minecraft.data.tags.ItemTagsProvider;
//import net.minecraft.world.level.block.Block;
//import net.neoforged.neoforge.common.data.ExistingFileHelper;
//
//import javax.annotation.Nullable;
//import java.util.concurrent.CompletableFuture;
//
//public class ModItemTagProvider extends ItemTagsProvider {
//    public ModItemTagProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider,
//                              CompletableFuture<TagLookup<Block>> pBlockTags, @Nullable ExistingFileHelper existingFileHelper) {
//        super(pOutput, pLookupProvider, pBlockTags, Boulanger.MOD_ID, existingFileHelper);
//    }
//
//    @Override
//    protected void addTags(HolderLookup.Provider pProvider) {
//        tag(ModTags.Items.TRANSFORMABLE_ITEMS)
//                .add(ModItems.BLACK_OPAL.get())
//    }
//}
