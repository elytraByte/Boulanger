package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.gui.screens.Screen;


import java.util.List;

public class WheatBushelItem extends Item {
    public WheatBushelItem(Properties props) {
        super(props);
    }

    /** Read the variety stored on this stack (defaults to HARD_RED_WINTER if absent). */
    public static WheatVariety getVariety(ItemStack stack) {
        WheatVariety v = stack.get(ModDataComponentTypes.WHEAT_VARIETY.get());
        return v != null ? v : WheatVariety.HARD_RED_WINTER;
    }

    /** Return a stack with the given variety already applied. */
    public static ItemStack withVariety(Item item, WheatVariety variety, int count) {
        ItemStack out = new ItemStack(item, count);
        out.set(ModDataComponentTypes.WHEAT_VARIETY.get(), variety);
        return out;
    }

    /** Ensure the stack has *some* variety set (if you ever need it). */
    public static void ensureVariety(ItemStack stack) {
        if (stack.get(ModDataComponentTypes.WHEAT_VARIETY.get()) == null) {
            stack.set(ModDataComponentTypes.WHEAT_VARIETY.get(), WheatVariety.HARD_RED_WINTER);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        WheatVariety v = getVariety(stack);
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.literal("Variety: ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(v.getSerializedName().replace('_', ' '))
                            .withStyle(ChatFormatting.YELLOW)));
        } else {
            tooltip.add(Component.literal("Hold ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("Shift").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" for variety").withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}