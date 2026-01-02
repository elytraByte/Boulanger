package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.content.WheatVariety;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class WheatSeedItem extends ItemNameBlockItem {

    public WheatSeedItem(Block block, Properties props) {
        super(block, props);
    }

    public static WheatVariety getVariety(ItemStack stack) {
        WheatVariety v = stack.get(ModDataComponentTypes.WHEAT_VARIETY.get());
        return v != null ? v : WheatVariety.HARD_RED_WINTER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        WheatVariety variety = getVariety(stack);
        tooltip.add(
                Component.literal("Wheat Variety: ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(pretty(variety.getSerializedName()))
                                .withStyle(ChatFormatting.YELLOW))

        );
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
