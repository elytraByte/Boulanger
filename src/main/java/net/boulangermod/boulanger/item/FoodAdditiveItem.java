package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.additive.FoodAdditiveType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FoodAdditiveItem extends Item {
    private final FoodAdditiveType additiveType;

    public FoodAdditiveItem(Properties properties, FoodAdditiveType type) {
        super(properties
                .component(ModDataComponentTypes.FOOD_ADDITIVE.get(), type.toComponent())
                .component(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), type.category()));
        this.additiveType = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        FoodAdditiveComponent comp = stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());

        // should always be present, but keep fallback for safety
        String id = (comp != null) ? comp.id() : additiveType.id();

        // show dynamic weighed amount if present; otherwise derive from enum unit weight * count
        WeightComponent weighed = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
        long mg = (weighed != null) ? weighed.milligrams() : additiveType.unitMg() * stack.getCount();

        tooltip.add(Component.literal("Additive: " + id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Weight: " + (mg / 1000.0) + " g").withStyle(ChatFormatting.GREEN));
    }

    @Override
    public Component getName(ItemStack stack) {
        FoodAdditiveComponent comp = stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
        String id = (comp != null) ? comp.id() : additiveType.id();
        return Component.translatable("item.boulanger.food_additive." + id);
    }


    public FoodAdditiveType getType() {
        return additiveType;
    }
}
