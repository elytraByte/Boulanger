package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.Nullable;

public enum DoughProcessInput implements RecipeInput {
    INSTANCE;

    private ItemStack doughStack;
    private ItemStack panStack;

    /** set both stacks before matching */
    public DoughProcessInput init(ItemStack dough, @Nullable ItemStack pan) {
        this.doughStack = dough;
        this.panStack   = pan;
        return this;
    }

    @Override
    public ItemStack getItem(int i) {
        // i == 0 → dough, i == 1 → pan
        return (i == 0 ? doughStack : panStack);
    }

    @Override
    public int size() {
        // we treat this as always 2-slot input
        return 2;
    }

    /**
     * @return the recipe‐type ID stored on the dough, or null if missing.
     */
    @Nullable
    public ResourceLocation getDoughType() {
        if (doughStack != null && doughStack.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {
            return doughStack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        }
        return null;
    }

    /** expose the pan stack for pan-type checks */
    @Nullable
    public ItemStack getPanStack() {
        return panStack;
    }
}

