package net.boulangermod.boulanger.util;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class IngredientStack {
    private final ItemStack bowlStack;          // original bowl (for metadata lookups)
    private final IngredientCategory category;
    private final Item actualItem;
    private final @Nullable FlourType flourType;

    // Store precise weight in milligrams
    private int milligrams;

    /** Construct from a bowl stack that already carries components (grams as float). */
    public IngredientStack(ItemStack bowlStack) {
        this.bowlStack = bowlStack;
        this.category = bowlStack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        this.actualItem = bowlStack.getItem();
        this.flourType = bowlStack.get(ModDataComponentTypes.FLOUR_TYPE.get());

        WeightComponent w = bowlStack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        float grams = (w != null) ? w.grams() : 0f;
        this.milligrams = Math.max(0, Math.round(grams * 1000f));
    }

    // Alternate ctor for restoring from NBT (mg exact)
    public IngredientStack(Item item, IngredientCategory cat, @Nullable FlourType ft, int milligrams) {
        this.bowlStack = ItemStack.EMPTY; // not needed for restored stacks
        this.actualItem = item;
        this.category = cat;
        this.flourType = ft;
        this.milligrams = Math.max(0, milligrams);
    }

    // --- Accessors ------------------------------------------------------------
    public ItemStack getBowlStack() { return bowlStack; }
    public IngredientCategory getCategory() { return category; }
    public Item getActualItem() { return actualItem; }
    public @Nullable FlourType getFlourType() { return flourType; }

    /** Exact weight in milligrams. */
    public int getMilligrams() { return milligrams; }

    /** Convenience: grams as double (mg / 1000). */
    public double getGramsExact() { return milligrams / 1000.0; }

    /** Backward-compat for old callers that used grams as int. */
    public int getGrams() { return (int)Math.round(getGramsExact()); }

    public void addMilligrams(int deltaMg) { this.milligrams = Math.max(0, this.milligrams + deltaMg); }
    public void addGrams(double grams) { addMilligrams((int)Math.round(grams * 1000.0)); }
}
