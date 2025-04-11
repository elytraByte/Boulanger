package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FlourItem extends Item {
    public FlourItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        WeightComponent weightComponent = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS);
        float weightToDisplay = (weightComponent != null) ? weightComponent.getWeight() : (type != null ? type.weight() : 0f);

        if (type != null) {
            tooltipComponents.add(Component.literal("Type: " + type.type()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("Ash: " + type.ash() + "%").withStyle(ChatFormatting.DARK_GRAY));
            tooltipComponents.add(Component.literal("Protein: " + type.protein() + "%").withStyle(ChatFormatting.BLUE));
            // Use the weight from the WeightComponent if it exists
            tooltipComponents.add(Component.literal("Weight: " + weightToDisplay + " g").withStyle(ChatFormatting.GREEN));
        } else {
            tooltipComponents.add(Component.literal("No flour data").withStyle(ChatFormatting.RED));
        }
    }


    @Override
    public Component getName(ItemStack stack) {
        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (type != null) {
            return Component.translatable("item.boulanger.flour." + type.type());
        }
        return super.getName(stack);
    }

}
