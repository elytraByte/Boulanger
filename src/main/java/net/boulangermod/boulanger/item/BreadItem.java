package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BreadItem extends Item {
    public BreadItem(Properties properties) {
        super(properties);
    }

    // ── NEW helpers ───────────────────────────────────────────────────────────
    private static String shortWeightLabelFromGrams(float grams) {
        int mg = Math.max(0, Math.round(grams * 1000f));
        if (mg < 1000) return mg + " mg";
        return String.format(Locale.ROOT, "%.3f g", mg / 1000.0);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // — Type line
        tooltipComponents.add(
                Component.literal("Type: " + getTypeName(stack))
                        .withStyle(ChatFormatting.GREEN)
        );

        // — Baker’s percentages header
        tooltipComponents.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.literal("Baker's Percentages").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));

        // — Flour breakdown
        tooltipComponents.add(Component.literal("Flours:").withStyle(ChatFormatting.GREEN));
        getFlourPercentages(stack).forEach((name, frac) -> {
            int pct = (int) Math.round(frac * 100);
            tooltipComponents.add(Component.literal(String.format(" * %d%% %s", pct, name)).withStyle(ChatFormatting.GREEN));
        });

        // — Other ingredients
        tooltipComponents.add(Component.literal("Other:").withStyle(ChatFormatting.GREEN));
        getOtherIngredientPercentages(stack).forEach((name, pctVal) -> {
            int pct = (int) Math.round(pctVal);
            tooltipComponents.add(Component.literal(String.format(" * %d%% %s", pct, name)).withStyle(ChatFormatting.GREEN));
        });

        // — Footer: Hydration & Weight (mg aware)
        tooltipComponents.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(
                Component.literal("Hydration: " + (int) Math.round(getHydration(stack)) + "%")
                        .withStyle(ChatFormatting.GREEN)
        );

        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        String weightLabel = (wComp != null)
                ? shortWeightLabelFromGrams(wComp.getWeight())
                : "0 g";
        tooltipComponents.add(
                Component.literal("Weight: " + weightLabel)
                        .withStyle(ChatFormatting.GREEN)
        );
    }


    // === Name from enum type (works in creative menu too) ====================

    @Override
    public Component getName(ItemStack stack) {
        BreadType bt = resolveBreadType(stack);
        if (bt != null) {
            // Use the enum id so "multigrain_bread" → "Multigrain Bread", "baguette" → "Baguette"
            return Component.literal(titleCaseTokens(bt.getId()));
        }
        return super.getName(stack);
    }

    private String getTypeName(ItemStack stack) {
        BreadType bt = resolveBreadType(stack);
        return (bt != null) ? titleCaseTokens(bt.getId()) : "Unknown";
    }

    /** Prefer component; otherwise infer from CustomModelData; else null. */
    private BreadType resolveBreadType(ItemStack stack) {
        var comp = stack.get(ModDataComponentTypes.BREAD_TYPE.get());
        if (comp != null) {
            // Component stores the enum (your existing code calls comp.name()), so return directly
            return comp;
        }
        var cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            int v = cmd.value();
            for (BreadType t : BreadType.values()) {
                if (t.getModelIndex() == v) return t;
            }
        }
        return null;
    }

    // — Helpers —

    private Map<String, Double> getFlourPercentages(ItemStack stack) {
        var recipe = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (recipe == null) return Collections.emptyMap();

        int totalFlour = recipe.ingredients().stream()
                .filter(i -> i.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::weight)
                .sum();
        if (totalFlour <= 0) return Collections.emptyMap();

        Map<String, Integer> gramsByType = new LinkedHashMap<>();
        for (IngredientInfo info : recipe.ingredients()) {
            if (info.category() != IngredientCategory.FLOUR) continue;

            FlourType ft = info.flourType();
            String key = (ft != null) ? ft.type() : info.itemId();
            gramsByType.merge(key, info.weight(), Integer::sum);
        }

        Map<String, Double> out = new LinkedHashMap<>();
        for (var e : gramsByType.entrySet()) {
            out.put(prettyFlourName(e.getKey()), e.getValue() / (double) totalFlour);
        }
        return out;
    }

    private static String prettyFlourName(String id) {
        String key = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)))
                        .append(p.length() > 1 ? p.substring(1) : "");
                if (i < parts.length - 1) sb.append(' ');
            }
        }
        return titleCaseTokens(id);
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

    private int getTotalWeight(ItemStack stack) {
        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        return (wComp != null)
                ? Math.round(wComp.getWeight())
                : 0;
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
