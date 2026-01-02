package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.IngredientInfo;
import net.boulangermod.boulanger.content.bread.BreadType;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.*;

public class BreadItem extends Item {
    public BreadItem(Properties properties) { super(properties); }

    private static String shortWeightLabelFromMg(int mg) {
        mg = Math.max(0, mg);
        if (mg < 1000) return mg + "mg";
        if (mg % 1000 == 0) return (mg / 1000) + "g";
        return String.format(Locale.ROOT, "%.1fg", mg / 1000.0);
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

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.literal("Hold ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("Shift").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" for ingredients & baker’s %").withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }

        tooltip.add(Component.literal("Ingredients (baker’s %)").withStyle(ChatFormatting.GREEN));

        Map<String, Double> flourPct = getFlourPercentages(stack);
        if (!flourPct.isEmpty()) {
            tooltip.add(Component.literal("• Flours").withStyle(ChatFormatting.GREEN));
            flourPct.forEach((name, frac) -> {
                int pct = (int)Math.round(frac * 100);
                tooltip.add(Component.literal(String.format(Locale.ROOT, "   - %d%% %s", pct, name))
                        .withStyle(ChatFormatting.GRAY));
            });
        }

        Map<String, Double> others = getOtherIngredientPercentages(stack);
        if (!others.isEmpty()) {
            tooltip.add(Component.literal("• Other").withStyle(ChatFormatting.GREEN));
            others.forEach((name, pctVal) -> {
                int pct = (int)Math.round(pctVal);
                tooltip.add(Component.literal(String.format(Locale.ROOT, "   - %d%% %s", pct, name))
                        .withStyle(ChatFormatting.GRAY));
            });
        }

        int hydration = (int)Math.round(getHydration(stack));
        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
        String weightLabel = (wComp != null) ? shortWeightLabelFromMg(wComp.milligrams()) : "0g";

        tooltip.add(Component.literal(String.format(Locale.ROOT, "Hydration: %d%%", hydration))
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Weight: " + weightLabel).withStyle(ChatFormatting.GREEN));
    }

    @Override
    public Component getName(ItemStack stack) {
        String baseName;
        BreadType bt = resolveBreadType(stack);
        if (bt != null) {
            baseName = titleCaseTokens(bt.id());
        } else {
            baseName = super.getName(stack).getString();
        }

        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
        if (wComp != null && wComp.milligrams() > 0) {
            String weight = shortWeightLabelFromMg(wComp.milligrams());
            return Component.literal(baseName + " (" + weight + ")");
        }
        return Component.literal(baseName);
    }

    private BreadType resolveBreadType(ItemStack stack) {
        BreadType bt = stack.get(ModDataComponentTypes.BREAD_TYPE.get());
        if (bt != null) return bt;

        var cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            int v = cmd.value();
            for (BreadType t : BreadType.values()) {
                if (t.modelIndex() == v) return t;
            }
        }
        return null;
    }

    private Map<String, Double> getFlourPercentages(ItemStack stack) {
        var recipe = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (recipe == null) return Collections.emptyMap();

        int totalFlourMg = recipe.ingredients().stream()
                .filter(i -> i.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::milligrams)
                .sum();
        if (totalFlourMg <= 0) return Collections.emptyMap();

        Map<String, Integer> mgByType = new LinkedHashMap<>();
        for (IngredientInfo info : recipe.ingredients()) {
            if (info.category() != IngredientCategory.FLOUR) continue;
            FlourType ft = info.flourType();
            String key = (ft != null) ? ft.id() : info.itemId();
            mgByType.merge(key, info.milligrams(), Integer::sum);
        }

        Map<String, Double> pctByType = new LinkedHashMap<>();
        for (var e : mgByType.entrySet()) {
            pctByType.put(titleCaseTokens(e.getKey()), (e.getValue() * 1.0) / totalFlourMg); // fraction 0..1
        }
        return pctByType;
    }

    private Map<String, Double> getOtherIngredientPercentages(ItemStack stack) {
        var pctComp = stack.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        if (pctComp == null) return Collections.emptyMap();

        Map<String, Double> map = new LinkedHashMap<>();
        for (var e : pctComp.percentages().entrySet()) {
            if (e.getKey() != IngredientCategory.FLOUR) {
                map.put(titleCaseTokens(e.getKey().name()), e.getValue());
            }
        }
        return map;
    }

    private double getHydration(ItemStack stack) {
        var pctComp = stack.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        if (pctComp != null && pctComp.percentages().containsKey(IngredientCategory.WATER)) {
            return pctComp.percentages().get(IngredientCategory.WATER);
        }
        return 0.0;
    }
}
