package net.boulangermod.boulanger.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;

public class MixingContainer implements Container, RecipeInput {
    private final ItemStack[] items;

    public MixingContainer(int size) {
        this.items = new ItemStack[size];
        Arrays.fill(this.items, ItemStack.EMPTY);
    }

    public MixingContainer(ItemStackHandler handler) {
        this(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            this.items[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
    }

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
        if (index < 0 || index >= items.length) return ItemStack.EMPTY;
        ItemStack s = items[index];
        return s == null ? ItemStack.EMPTY : s;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index < 0 || index >= items.length || count <= 0) return ItemStack.EMPTY;

        ItemStack stack = getItem(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack taken = stack.split(count);
        if (stack.isEmpty()) items[index] = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index < 0 || index >= items.length) return ItemStack.EMPTY;

        ItemStack stack = getItem(index);
        items[index] = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= items.length) return;
        items[index] = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        Arrays.fill(this.items, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return items.length;
    }
}
