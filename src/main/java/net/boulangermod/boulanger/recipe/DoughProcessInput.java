package net.boulangermod.boulanger.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public enum DoughProcessInput implements RecipeInput {
    INSTANCE;

    @Override
    public ItemStack getItem(int i) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }
}
