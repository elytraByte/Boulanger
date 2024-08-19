package org.l3e.boulanger.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.item.ModItems;

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

    }
}
