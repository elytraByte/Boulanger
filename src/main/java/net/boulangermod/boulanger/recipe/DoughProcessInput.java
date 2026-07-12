package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import javax.annotation.Nullable;

/**
 * Immutable recipe input for dough-processing recipes.
 * Construct a fresh instance per lookup to avoid shared mutable state.
 */
public record DoughProcessInput(ItemStack dough, ItemStack pan) implements RecipeInput {

    public DoughProcessInput {
        // Normalize null/empty stacks to EMPTY to preserve old behavior
        dough = (dough == null || dough.isEmpty()) ? ItemStack.EMPTY : dough;
        pan   = (pan   == null || pan.isEmpty())   ? ItemStack.EMPTY : pan;
    }

    /** Convenience factory (optional, but helps readability at call sites). */
    public static DoughProcessInput of(ItemStack dough, @Nullable ItemStack pan) {
        return new DoughProcessInput(dough, pan);
    }

    @Override
    public ItemStack getItem(int i) {
        return (i == 0) ? dough : (i == 1 ? pan : ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return 2;
    }

    /** ID-based linkage: reads the process recipe id stored on the dough stack. */
    public @Nullable ResourceLocation getDoughType() {
        if (!dough.isEmpty() && dough.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {
            return dough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        }
        return null;
    }

    /** Back-compat helper for existing call sites that used getPanStack(). */
    public ItemStack getPanStack() {
        return pan;
    }
}
