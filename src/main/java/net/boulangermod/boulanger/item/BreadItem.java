package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.BreadType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class BreadItem extends Item {
    public BreadItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        BreadType type = stack.get(ModDataComponentTypes.BREAD_TYPE.get());
        if (type != null) {
            return Component.translatable("item.boulanger.bread." + type.getId());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        BreadType type = stack.get(ModDataComponentTypes.BREAD_TYPE.get());
        WeightComponent weightComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS);

        if (type != null) {
            // Type line
            tooltipComponents.add(
                    Component.literal("Type: " + type.name())
                            .withStyle(ChatFormatting.GRAY)
            );

            // Weight line, only if we have the data
            if (weightComp != null) {
                float weight = weightComp.getWeight();
                tooltipComponents.add(
                        Component.literal("Weight: " + weight + " g")
                                .withStyle(ChatFormatting.GREEN)
                );
            } else {
                tooltipComponents.add(
                        Component.literal("Weight: unknown")
                                .withStyle(ChatFormatting.DARK_GRAY)
                );
            }
        } else {
            tooltipComponents.add(
                    Component.literal("Unbaked dough?").withStyle(ChatFormatting.RED)
            );
        }
    }

}
