package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.item.BreadType;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.FlourItemType;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, Boulanger.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        // Flour base model with all overrides
        ItemModelBuilder flour = withExistingParent("flour", "item/generated")
                .texture("layer0", modLoc("item/flour")); // base flour.png

        for (FlourItemType type : FlourItemType.values()) {
            // Override for custom_model_data
            flour.override()
                    .predicate(ResourceLocation.fromNamespaceAndPath("minecraft","custom_model_data"), type.getModelIndex())
                    .model(withExistingParent("flour/" + type.getId(), "item/generated"))
                    // flour/first_break_flour
                    .end();

            // Individual override model: flour/first_break_flour.json
            withExistingParent("flour/" + type.getId(), "item/generated")
                    .texture("layer0", modLoc("item/flour/" + type.getId()));
        }

        //breads
        ItemModelBuilder bread = withExistingParent("bread", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/bread"));

        for (BreadType type : BreadType.values()) {
            String id = type.getId();
            String modelPath = "bread/" + id;

            // build it once, capture it
            ItemModelBuilder overrideModel = withExistingParent(modelPath, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + id));

            // reference _that_ builder in your override
            bread.override()
                    .predicate(mcLoc("custom_model_data"), type.getModelIndex())
                    .model(overrideModel)
                    .end();
        }

        basicItem(ModItems.BUTTER.get());
        basicItem(ModItems.FANCY_EGG.get());
        basicItem(ModItems.WOODEN_BUCKET.get());
        basicItem(ModItems.WOODEN_BUCKET_OF_SHELL_EGG.get());
        basicItem(ModItems.WOODEN_BUCKET_OF_WHOLE_MILK.get());
        basicItem(ModItems.BROWN_SUGAR.get());
        basicItem(ModItems.SALT_KOSHER.get());
        basicItem(ModItems.SOURDOUGH_STARTER.get());
        basicItem(ModItems.RYE_SOUR_STARTER.get());
        basicItem(ModItems.SOYBEAN_OIL.get());
        basicItem(ModItems.CANOLA_OIL.get());
        basicItem(ModItems.MOLASSES.get());
        basicItem(ModItems.POWDERED_SUGAR.get());
        basicItem(ModItems.EGG_YOLK.get());
        basicItem(ModItems.EGG_WHITE.get());
        basicItem(ModItems.WHOLE_MILK.get());
        basicItem(ModItems.EURO_BUTTER.get());
        basicItem(ModItems.EURO_BUTTER_BLEND.get());
        basicItem(ModItems.SAF_RED.get());
        basicItem(ModItems.SAF_GOLD.get());
        basicItem(ModItems.BREWERS_YEAST.get());
        basicItem(ModItems.FLEISCHMANN.get());
        basicItem(ModItems.FRESH_YEAST.get());
        basicItem(ModItems.WHEAT_BERRIES.get());
        basicItem(ModItems.WHEAT_SEED.get());
        basicItem(ModItems.HARD_RED_SPRING_WHEAT.get());
        basicItem(ModItems.DOUGH.get());
        basicItem(ModItems.KAOLINITE_CLAY_BALL.get());
        basicItem(ModItems.PORCELAIN_MIX.get());
        basicItem(ModItems.BRICK_MOLD.get());
        basicItem(ModItems.DIORITE_BRICK.get());
        basicItem(ModItems.DIORITE_PLATE.get());
        basicItem(ModItems.PINE_RESIN.get());
        basicItem(ModItems.COPPER_CHANNEL.get());
        basicItem(ModItems.PCB.get());
        basicItem(ModItems.GILDED_PCB.get());
        basicItem(ModItems.REINFORCED_DIORITE_PLATE.get());
        basicItem(ModItems.UNFIRED_PORCELAIN_BRICK.get());
        basicItem(ModItems.PORCELAIN_BRICK.get());
        basicItem(ModItems.GLASS_DUST.get());
        basicItem(ModItems.BONE_ASH.get());
        basicItem(ModItems.SPLIT_PINE_LOGS.get());
        basicItem(ModItems.SLEDGEHAMMER.get());
        basicItem(ModItems.IRON_WEDGE.get());
        basicItem(ModItems.HOLSTEIN_FRIESIAN_COW_SPAWN_EGG.get());
        basicItem(ModItems.HEN_SPAWN_EGG.get());
        basicItem(ModItems.FILLED_BOWL_ITEM.get());
        saplingItem(ModBlocks.PINE_SAPLING);
        fenceItem(ModBlocks.PINE_FENCE, ModBlocks.PINE_PLANKS);


        withExistingParent("wood_oven", modLoc("block/wood_oven_off"));
        withExistingParent("mixing_block", modLoc("block/mixing_block"));
        withExistingParent("stone_mill_block", modLoc("block/stone_mill_block"));
        withExistingParent("scale_block", modLoc("block/scale_block"));
    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                        "block/" + item.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                        "block/" + baseBlock.getId().getPath()));
    }


}
