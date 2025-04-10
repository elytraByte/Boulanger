package net.boulangermod.boulanger.util;

import net.minecraft.world.item.Item;

public class IngredientStack {
    private int grams;
    private final Item item;
    private final IngredientCategory category;


    public IngredientStack(Item item, IngredientCategory category, int grams) {
        this.item = item;
        this.category = category;
        this.grams = grams;
    }

    public Item item() { return item; }
    public IngredientCategory category() { return category; }
    public int grams() { return grams; }


    public void addGrams(int amount) {
        this.grams += amount;
    }
}

