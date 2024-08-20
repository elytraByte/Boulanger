package org.l3e.boulanger.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.block.ModBlocks;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, Boulanger.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        //basicItem(ModItems.DOUGH.get());
        //basicItem(ModItems.FLOUR_AP.get());
        //basicItem(ModItems.FLOUR_WW.get());
        //basicItem(ModItems.SALT_KOSHER.get());
        //basicItem(ModItems.YEAST_BREWERS.get());
        //basicItem(ModItems.HARD_RED_SPRING_WHEAT.get());
        //basicItem(ModItems.HARD_RED_SPRING_WHEAT_SEEDS.get());

        saplingItem(ModBlocks.PINE_SAPLING);

    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                        "block/" + item.getId().getPath()));
    }
}
