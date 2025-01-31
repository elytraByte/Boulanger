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
import org.l3e.boulanger.item.ModItems;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, Boulanger.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.WHEAT_BERRIES.get());
        basicItem(ModItems.BRAN.get());
        basicItem(ModItems.BREAK_FLOUR.get());
        basicItem(ModItems.MIDDLINGS_FLOUR.get());
        basicItem(ModItems.PATENT_FLOUR.get());
        basicItem(ModItems.RYE_FLOUR.get());
        basicItem(ModItems.SEMOLINA_FLOUR.get());
        basicItem(ModItems.WHOLE_WHEAT_FLOUR.get());
        basicItem(ModItems.ALL_PURPOSE_FLOUR.get());
        basicItem(ModItems.BREAD_FLOUR.get());
        basicItem(ModItems.HIGH_GLUTEN_FLOUR.get());
        basicItem(ModItems.VITAL_WHEAT_GLUTEN.get());
        basicItem(ModItems.FIFTY_POUND_FLOUR.get());
        basicItem(ModItems.BUTTER.get());
        basicItem(ModItems.EURO_BUTTER.get());
        basicItem(ModItems.EURO_BUTTER_BLEND.get());
        basicItem(ModItems.SAF_RED.get());
        basicItem(ModItems.SAF_GOLD.get());
        basicItem(ModItems.FLEISCHMANN.get());
        basicItem(ModItems.FRESH_YEAST.get());
        saplingItem(ModBlocks.PINE_SAPLING);

    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                        "block/" + item.getId().getPath()));
    }
}
