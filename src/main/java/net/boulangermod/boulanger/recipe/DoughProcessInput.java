package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import javax.annotation.Nullable;

public enum DoughProcessInput implements RecipeInput {
    INSTANCE;

    private ItemStack doughStack = ItemStack.EMPTY;
    private ItemStack panStack   = ItemStack.EMPTY;

    /** set both stacks before matching */
    public DoughProcessInput init(ItemStack dough, @Nullable ItemStack pan) {
        this.doughStack = (dough == null || dough.isEmpty()) ? ItemStack.EMPTY : dough;
        this.panStack   = (pan == null   || pan.isEmpty())   ? ItemStack.EMPTY : pan;
        return this;
    }

    @Override
    public ItemStack getItem(int i) {
        return (i == 0 ? doughStack : panStack);
    }

    @Override
    public int size() {
        return 2;
    }

    @Nullable
    public ResourceLocation getDoughType() {
        if (!doughStack.isEmpty() && doughStack.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {
            return doughStack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        }
        return null;
    }

    public ItemStack getPanStack() {
        return panStack;
    }

    public void clear() {
        this.doughStack = ItemStack.EMPTY;
        this.panStack = ItemStack.EMPTY;
    }
}
