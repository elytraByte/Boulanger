package net.boulangermod.boulanger.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BrickMoldItem extends Item {
    public BrickMoldItem(Properties props) {
        super(props);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack mold = stack.copy();
        int next = mold.getDamageValue() + 1;
        if (next >= mold.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        mold.setDamageValue(next);
        return mold;
    }
}