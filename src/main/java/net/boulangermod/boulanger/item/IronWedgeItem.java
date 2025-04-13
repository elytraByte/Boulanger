package net.boulangermod.boulanger.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class IronWedgeItem extends BlockItem {
    public IronWedgeItem(Block block, Properties props) {
        super(block, props.stacksTo(1).durability(512));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (1.0F - (float)stack.getDamageValue() / stack.getMaxDamage()));
    }
}