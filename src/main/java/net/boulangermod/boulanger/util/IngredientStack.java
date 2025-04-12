package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class IngredientStack {
    private final ItemStack bowlStack;
    private final Item       actualItem;
    private final IngredientCategory category;
    private int grams;

    public IngredientStack(ItemStack bowlStack) {
        this.bowlStack = bowlStack;
        // category & grams come from components:
        this.category = bowlStack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        WeightComponent wc = bowlStack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        this.grams    = wc != null ? (int)wc.grams() : 0;
        // actual item from the type component:
        var typeComp = bowlStack.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        this.actualItem = typeComp != null ? typeComp.item() : bowlStack.getItem();
    }

    /** The bowl ItemStack (with data components). */
    public ItemStack getBowlStack() {
        return bowlStack;
    }

    /** The real ingredient item (flour, water, etc.). */
    public Item getActualItem() {
        return actualItem;
    }

    /** Category (FLOUR, FAT, etc.) */
    public IngredientCategory getCategory() {
        return category;
    }

    /** Weight in grams */
    public int getGrams() {
        return grams;
    }

    /** Add more grams (used when merging multiple bowls) */
    public void addGrams(int delta) {
        this.grams += delta;
    }

    /** FlourType component, if any */
    public FlourType getFlourType() {
        return bowlStack.get(ModDataComponentTypes.FLOUR_TYPE.get());
    }
}
