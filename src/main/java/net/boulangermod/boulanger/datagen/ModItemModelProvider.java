package net.boulangermod.boulanger.datagen;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.content.bread.BreadType;
import net.boulangermod.boulanger.content.flour.FiftyPoundBagType;
import net.boulangermod.boulanger.content.flour.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
        super(output, Boulanger.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.WHEAT_SEEDS.get());
        basicItem(ModItems.WHEAT_BUSHEL.get());
        basicItem(ModItems.WHEAT_BERRIES.get());
        basicItem(ModItems.PINE_RESIN.get());
        basicItem(ModItems.SPLIT_PINE_LOGS.get());
        basicItem(ModItems.KAOLINITE_CLAY_BALL.get());
        basicItem(ModItems.BRICK_MOLD.get());
        basicItem(ModItems.FILLED_BOWL.get());
        folderItem(ModItems.KOSHER_SALT.getId(), "item/salt");
        folderItem(ModItems.MOLASSES.getId(), "item/sugar");
        folderItem(ModItems.BROWN_SUGAR.getId(), "item/sugar");
        folderItem(ModItems.POWDERED_SUGAR.getId(), "item/sugar");
        folderItem(ModItems.MARGARINE.getId(), "item/fat");
        folderItem(ModItems.LARD.getId(), "item/fat");
        folderItem(ModItems.BUTTER.getId(), "item/fat");
        folderItem(ModItems.EUROPEAN_BUTTER.getId(), "item/fat");
        folderItem(ModItems.EUROPEAN_BUTTER_BLEND.getId(), "item/fat");
        folderItem(ModItems.BUTTER_SALTED.getId(), "item/fat");
        folderItem(ModItems.EUROPEAN_BUTTER_SALTED.getId(), "item/fat");
        folderItem(ModItems.EUROPEAN_BUTTER_BLEND_SALTED.getId(), "item/fat");
        folderItem(ModItems.SAF_RED_YEAST.getId(), "item/yeast");
        folderItem(ModItems.SAF_GOLD_YEAST.getId(), "item/yeast");
        folderItem(ModItems.FLEISCHMANNS_YEAST.getId(), "item/yeast");
        folderItem(ModItems.BREWERS_YEAST.getId(), "item/yeast");
        folderItem(ModItems.FRESH_YEAST.getId(), "item/yeast");
        folderItem(ModItems.RYE_SOUR_STARTER.getId(), "item/yeast");
        folderItem(ModItems.SOURDOUGH_STARTER.getId(), "item/yeast");
        folderItem(ModItems.S_500_RED.getId(), "item/bakery_additive");
        folderItem(ModItems.IM_PROVE_200.getId(), "item/bakery_additive");
        folderItem(ModItems.ADVANTAGE_500_CL.getId(), "item/bakery_additive");
        folderItem(ModItems.SOYBEAN_OIL.getId(), "item/fat");
        folderItem(ModItems.CANOLA_OIL.getId(), "item/fat");
        folderItem(ModItems.DRY_WHOLE_MILK_POWDER.getId(), "item/dairy");
        folderItem(ModItems.DRY_BUTTERMILK_POWDER.getId(), "item/dairy");

//pneumatic ducting deprecated for the time being
//        basicItem(ModItems.BLIND_FLANGE.get());

        ItemModelBuilder flour = withExistingParent("flour", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/flour/flour"));

        for (FlourItemType type : FlourItemType.values()) {
            String idPath = pathOnly(type.getId());

            flour.override()
                    .predicate(mcLoc("custom_model_data"), type.getModelIndex())
                    .model(withExistingParent("item/flour/" + idPath, mcLoc("item/generated")))
                    .end();

            withExistingParent("item/flour/" + idPath, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/flour/" + idPath));
        }

        ItemModelBuilder bread = withExistingParent("bread", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/bread/bread"));

        for (BreadType type : BreadType.values()) {
            String id = type.id();
            ItemModelBuilder overrideModel = withExistingParent("item/bread/" + id, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/bread/" + id));

            bread.override()
                    .predicate(mcLoc("custom_model_data"), type.modelIndex())
                    .model(overrideModel)
                    .end();
        }

        ItemModelBuilder fiftyBag = withExistingParent("fifty_pound_bag", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/flour/blank_fifty_pound_bag"));

        for (FiftyPoundBagType type : FiftyPoundBagType.values()) {
            String basePath = pathOnly(type.id().toString());
            String modelPath = basePath + "_fifty_pound_bag";
            int modelIndex = type.modelIndex();

            ItemModelBuilder overrideModel = withExistingParent("item/flour/" + modelPath, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/flour/" + modelPath));

            fiftyBag.override()
                    .predicate(mcLoc("custom_model_data"), modelIndex)
                    .model(overrideModel)
                    .end();
        }
    }

    /**
     * Converts "boulanger:all_purpose_flour" -> "all_purpose_flour"
     * If already not namespaced, returns unchanged.
     */
    private static String pathOnly(String maybeNamespacedId) {
        ResourceLocation rl = ResourceLocation.tryParse(maybeNamespacedId);
        return rl != null ? rl.getPath() : maybeNamespacedId;
    }

    private ItemModelBuilder folderItem(ResourceLocation itemId, String textureDirectory) {
        String itemPath = itemId.getPath();

        return withExistingParent(itemPath, mcLoc("item/generated"))
                .texture("layer0", modLoc(textureDirectory + "/" + itemPath));
    }
}
