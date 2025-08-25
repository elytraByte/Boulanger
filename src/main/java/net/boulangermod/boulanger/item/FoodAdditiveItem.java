package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FoodAdditiveItem extends Item {
    private final FoodAdditiveType additiveType;

    public FoodAdditiveItem(Properties properties, FoodAdditiveType additiveType) {
        super(properties);
        this.additiveType = additiveType;
    }

    /**
     * Ensures the FoodAdditiveComponent is attached to this stack.
     * If missing, it uses the additiveType value to create and set the component.
     */
    private FoodAdditiveComponent ensureComponent(ItemStack stack) {
        FoodAdditiveComponent component = stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
        if (component == null) {
            component = additiveType.toFoodAdditiveComponent();
            stack.set(ModDataComponentTypes.FOOD_ADDITIVE.get(), component);
        }
        return component;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        FoodAdditiveComponent component = ensureComponent(stack);
        // Try to get the dynamic weight from the WeightComponent on the stack.
        WeightComponent weightComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        float currentWeight = (weightComp != null) ? weightComp.getWeight() : component.getWeight();
        tooltipComponents.add(Component.literal("Additive: " + component.getId()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Weight: " + currentWeight + " g").withStyle(ChatFormatting.GREEN));
    }

    @Override
    public Component getName(ItemStack stack) {
        FoodAdditiveComponent component = ensureComponent(stack);
        return Component.translatable("item.boulanger.food_additive." + component.getId());
    }

    public FoodAdditiveType getType() {
        return additiveType;
    }
}
