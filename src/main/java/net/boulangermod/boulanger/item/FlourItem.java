package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FlourItem extends Item {
    public FlourItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (type == null) {
            tooltip.add(Component.literal("No flour data").withStyle(ChatFormatting.RED));
            return;
        }

        String idPath = pathOnly(type.id());
        String prettyName = pretty(idPath);
        String label;
        if (prettyName.endsWith(" Flour")) {
            label = "Flour Type: ";
            prettyName = prettyName.substring(0, prettyName.length() - 6);
        } else {
            label = "Type: ";
        }

        long unitMg = type.unitMg();
        long totalMg = type.totalMilligrams(stack);

        tooltip.add(Component.literal(label + prettyName).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("Ash: %.2f%%", type.ash())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(String.format("Protein: %.1f%%", type.protein())).withStyle(ChatFormatting.BLUE));

        tooltip.add(Component.literal(String.format("Unit: %.1fg", unitMg / 1000.0)).withStyle(ChatFormatting.GREEN));
        if (stack.getCount() > 1) {
            tooltip.add(Component.literal(String.format("Total: %.1fg", totalMg / 1000.0)).withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (type == null) return super.getName(stack);

        String idPath = pathOnly(type.id());
        return Component.translatable("item.boulanger.flour." + idPath);
    }

    private static String pathOnly(String maybeNamespaced) {
        ResourceLocation rl = ResourceLocation.tryParse(maybeNamespaced);
        return rl != null ? rl.getPath() : maybeNamespaced;
    }

    private static String pretty(String idPath) {
        String[] parts = idPath.split("_");
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