package net.boulangermod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;



import java.util.List;

public class BigFlour extends Item {

    private static final int MAX_GRAMS = 22680; // Total grams in a 50lb bag

    public BigFlour(Properties properties) {
        super(properties.durability(MAX_GRAMS)); // Set max durability to 22,680
    }
    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {

        int remainingFlour = MAX_GRAMS - pStack.getDamageValue();

        pTooltipComponents.add(Component.translatable("tooltip.boulanger.big_flour.desc.1", remainingFlour + "g"));
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    /**
     * Custom method to deduct flour usage.
     * @param stack The flour bag item stack.
     * @param gramsUsed The grams of flour used.
     */
    public void useFlour(ItemStack stack, int gramsUsed) {
        int currentDamage = stack.getDamageValue();
        if (currentDamage + gramsUsed > MAX_GRAMS) {
            gramsUsed = MAX_GRAMS - currentDamage; // Clamp usage
        }
        stack.setDamageValue(currentDamage + gramsUsed); // Apply usage
    }


    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true; // Show durability bar for remaining flour
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int remainingFlour = MAX_GRAMS - stack.getDamageValue();
        return Math.round(13.0F * remainingFlour / MAX_GRAMS); // Scale bar width
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float durabilityPercentage = 1.0F - ((float) stack.getDamageValue() / stack.getMaxDamage());

        // Gradient: Green to Red (full to empty)
        int red = (int) ((1.0F - durabilityPercentage) * 255);
        int green = (int) (durabilityPercentage * 255);
        return (red << 16) | (green << 8); // Combine red and green into RGB
    }

    @Override
    public int getDefaultMaxStackSize() {
        return 1;
    }
}
