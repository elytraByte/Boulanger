package net.boulangermod.boulanger.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;
import java.util.List;

/**
 * A simple container wrapper around an ItemStackHandler (or raw ItemStack[])
 * that RecipeManager can query when matching/assembling MixingRecipes.
 */
public class MixingContainer implements Container, RecipeInput {
    private final ItemStack[] items;

    /** Create an empty MixingContainer of the given size. */
    public MixingContainer(int size) {
        this.items = new ItemStack[size];
        Arrays.fill(this.items, ItemStack.EMPTY);
    }

    /**
     * Wrap an existing ItemStackHandler (e.g. your MixingBlockEntity.inventory).
     * Copies a snapshot of the handler’s stacks into a plain array for recipe matching.
     */
    public MixingContainer(ItemStackHandler handler) {
        this(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            this.items[i] = handler.getStackInSlot(i);
        }
    }

    // --- Container methods ---

    @Override
    public int getContainerSize() {
        return items.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= items.length) {
            return ItemStack.EMPTY;
        }
        return items[index];
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return ContainerHelper.removeItem(List.of(items), index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(List.of(items), index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= items.length) return;
        items[index] = stack;
    }

    @Override
    public void setChanged() {
        // No‐op: snapshot container
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        Arrays.fill(this.items, ItemStack.EMPTY);
    }

    // --- RecipeInput methods ---

    /** Number of slots / items in this input. */
    @Override
    public int size() {
        return items.length;
    }

    public ItemStack get(int i) {
        return getItem(i);
    }

    public ItemStack remove(int i, int count) {
        return removeItem(i, count);
    }

    public ItemStack remove(int i) {
        return removeItemNoUpdate(i);
    }

    public void set(int i, ItemStack stack) {
        setItem(i, stack);
    }

    public void clear() {
        clearContent();
    }
}
