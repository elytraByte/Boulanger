package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.item.*;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, Boulanger.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // ─── Flour with overrides via custom_model_data ───────────────────────
        ItemModelBuilder flour = withExistingParent("flour", "item/generated")
                .texture("layer0", modLoc("item/flour"));

        for (FlourItemType type : FlourItemType.values()) {
            flour.override()
                    .predicate(ResourceLocation.fromNamespaceAndPath("minecraft", "custom_model_data"), type.getModelIndex())
                    .model(withExistingParent("flour/" + type.getId(), "item/generated"))
                    .end();

            withExistingParent("flour/" + type.getId(), "item/generated")
                    .texture("layer0", modLoc("item/flour/" + type.getId()));
        }

        // ─── Bread with overrides ─────────────────────────────────────────────
        ItemModelBuilder bread = withExistingParent("bread", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/bread"));

        for (BreadType type : BreadType.values()) {
            String id = type.getId();
            ItemModelBuilder overrideModel = withExistingParent("bread/" + id, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + id));

            bread.override()
                    .predicate(mcLoc("custom_model_data"), type.getModelIndex())
                    .model(overrideModel)
                    .end();
        }

        // ─── Fifty Pound Bags (overrides) ─────────────────────────────────────
        ItemModelBuilder fiftyBag = withExistingParent("fifty_pound_bag", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/blank_fifty_pound_bag"));

        for (FiftyPoundBagType type : FiftyPoundBagType.values()) {
            String id = type.getId() + "_fifty_pound_bag";
            int modelIndex = type.getModelIndex();

            ItemModelBuilder overrideModel = withExistingParent(id, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + id));

            fiftyBag.override()
                    .predicate(mcLoc("custom_model_data"), modelIndex)
                    .model(overrideModel)
                    .end();
        }

        // ─── Pans with overrides (empty/full) ─────────────────────────────────
        ItemModelBuilder pan = withExistingParent("pan", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/loaf_pan"));

        for (PanType type : PanType.values()) {
            String id = type.getId();
            int empty = type.getEmptyModelIndex();
            int full  = type.getFullModelIndex();

            ItemModelBuilder emptyModel = withExistingParent("pan/" + id + "/empty", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + id + "_pan"));
            ItemModelBuilder fullModel = withExistingParent("pan/" + id + "/full", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + id + "_pan_full"));

            pan.override().predicate(mcLoc("custom_model_data"), empty).model(emptyModel).end();
            pan.override().predicate(mcLoc("custom_model_data"), full ).model(fullModel ).end();
        }

        // ─── Simple items (generated) ─────────────────────────────────────────
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
        basicItem(ModItems.UNFIRED_BLACK_PORCELAIN_BRICK.get());
        basicItem(ModItems.UNFIRED_BLUE_PORCELAIN_BRICK.get());
        basicItem(ModItems.UNFIRED_LIGHT_BLUE_PORCELAIN_BRICK.get());
        basicItem(ModItems.BLUE_PORCELAIN_BRICK.get());
        basicItem(ModItems.LIGHT_BLUE_PORCELAIN_BRICK.get());
        basicItem(ModItems.BLACK_PORCELAIN_BRICK.get());
        basicItem(ModItems.PORCELAIN_BRICK.get());
        basicItem(ModItems.GLASS_DUST.get());
        basicItem(ModItems.BONE_ASH.get());
        basicItem(ModItems.SPLIT_PINE_LOGS.get());
        basicItem(ModItems.SLEDGEHAMMER.get());
        basicItem(ModItems.IRON_WEDGE.get());
        basicItem(ModItems.HOLSTEIN_FRIESIAN_COW_SPAWN_EGG.get());
        basicItem(ModItems.HEN_SPAWN_EGG.get());
        basicItem(ModItems.FILLED_BOWL_ITEM.get());
        basicItem(ModItems.GASIFIER_FILTER.get());
        basicItem(ModItems.MILLIGRAM_SCALE.get());

        // ─── Block items that should show the block’s 3D model ───────────────
        // (pipes/cables often use simple item models; adjust if you prefer block parents)
        basicItem(ModBlocks.ENERGY_CABLE.asItem());
        basicItem(ModBlocks.WOODGAS_PIPE.asItem());

        // ─── Sapling & fence inventory items ─────────────────────────────────
        saplingItem(ModBlocks.PINE_SAPLING);
        fenceItem(ModBlocks.PINE_FENCE, ModBlocks.PINE_PLANKS);

        // ─── Stairs/Slab/Gate items parented to their block models ───────────
        withExistingParent(ModBlocks.PINE_STAIRS.getId().getPath(), modLoc("block/pine_stairs"));
        withExistingParent(ModBlocks.PINE_SLAB.getId().getPath(),   modLoc("block/pine_slab"));
        withExistingParent(ModBlocks.PINE_FENCE_GATE.getId().getPath(), modLoc("block/pine_fence_gate"));

        // ─── Machine-style block items (isometric GUI view, scaled down) ─────
        machineItem("wood_oven",      "block/wood_oven_off",   0.62F);
        machineItem("mixing_block",   "block/mixing_block",    0.62F);
        machineItem("stone_mill",     "block/stone_mill_block",0.62F);
        machineItem("scale_block",    "block/scale1",     0.62F); // if your block model is 'scale1', change to "block/scale1"
        machineItem("proofing_box",   "block/proofing_box",    0.62F);
        machineItem("machine_housing","block/machine_housing", 0.62F);
        machineItem("dough_divider",  "block/dough_divider",   0.62F);
        machineItem("iron_frame",     "block/iron_frame_0",    0.62F);
        machineItem("gas_tank",     "block/gas_tank",    0.62F);
        machineItem("woodgas_engine",     "block/woodgas_engine_off",    0.62F);
    }

    // Helper to make a nice isometric block-item render using the block model as parent
    private ItemModelBuilder machineItem(String itemId, String blockModelPath, float guiScale) {
        ItemModelBuilder b = withExistingParent(itemId, modLoc(blockModelPath));
        b.transforms()
                .transform(ItemDisplayContext.GUI)
                .rotation(30, 225, 0).scale(guiScale)
                .end()
                .transform(ItemDisplayContext.GROUND)
                .translation(0, 3, 0).scale(0.35F)
                .end()
                .transform(ItemDisplayContext.FIXED)
                .scale(0.50F)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.40F)
                .end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.40F)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(0, 45, 0).scale(0.55F)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 315, 0).scale(0.55F)
                .end()
                .end();
        return b;
    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(), ResourceLocation.parse("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                        "block/" + item.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture", ResourceLocation.fromNamespaceAndPath(Boulanger.MODID,
                        "block/" + baseBlock.getId().getPath()));
    }
}
