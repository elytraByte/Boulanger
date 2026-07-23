package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.BakeryAdditiveComponent;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.additive.BakeryAdditiveType;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class BakeryAdditiveItem extends Item {
    private final BakeryAdditiveType type;

    public BakeryAdditiveItem(Properties properties, BakeryAdditiveType type) {
        super(properties
                .component(ModDataComponentTypes.BAKERY_ADDITIVE.get(), type.toComponent())
                .component(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), type.category()));
        this.type = type;
    }

    public BakeryAdditiveType getType() { return type; }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        BakeryAdditiveComponent add = stack.get(ModDataComponentTypes.BAKERY_ADDITIVE.get());
        String id = (add != null) ? add.id() : type.id();

        IngredientCategory cat = stack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        if (cat == null) cat = type.category();

        // Dynamic weighed weight (Scale etc.) wins; otherwise unitMg * count
        WeightComponent wComp = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
        int totalMg = (wComp != null) ? wComp.milligrams() : type.unitMg() * stack.getCount();

        tooltip.add(Component.literal("Additive: " + titleCaseTokens(id)).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Category: " + titleCaseTokens(cat.name())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Weight: " + formatGrams(totalMg)).withStyle(ChatFormatting.GREEN));
    }

    @Override
    public Component getName(ItemStack stack) {
        BakeryAdditiveComponent add = stack.get(ModDataComponentTypes.BAKERY_ADDITIVE.get());
        String id = (add != null) ? add.id() : type.id();
        return Component.translatable("item.boulanger.bakery_additive." + id);
    }

    private static String formatGrams(int mg) {
        mg = Math.max(0, mg);
        if (mg < 1000) return mg + " mg";
        if (mg % 1000 == 0) return (mg / 1000) + " g";
        return String.format(Locale.ROOT, "%.1f g", mg / 1000.0);
    }

    private static String titleCaseTokens(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String key = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)))
                        .append(p.length() > 1 ? p.substring(1) : "");
                if (i < parts.length - 1) sb.append(' ');
            }
        }
        return sb.toString();
    }
}
