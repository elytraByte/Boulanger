package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.content.WheatVariety;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WheatBushelItem extends Item {

    public WheatBushelItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        WheatVariety variety = stack.get(ModDataComponentTypes.WHEAT_VARIETY.get());
        if (variety == null) {
            tooltip.add(Component.literal("Wheat Variety: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("MISSING").withStyle(ChatFormatting.RED)));
            return;
        }

        tooltip.add(Component.literal("Wheat Variety: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(pretty(variety.getSerializedName()))
                        .withStyle(ChatFormatting.YELLOW)));
    }

    private static String pretty(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
