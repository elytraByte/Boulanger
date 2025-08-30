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
import java.util.*;

public class BreadItem extends Item {
    public BreadItem(Properties properties) { super(properties); }

    private static String shortWeightLabelFromGrams(float grams) {
        int mg = Math.max(0, Math.round(grams * 1000f));
        if (mg < 1000) return mg + "mg";
        int g = Math.round(mg / 1000f);
        return g + "g";
    }

    /** Format a per-ingredient weight stored in milligrams (compact). */
    private static String shortWeightLabelFromMilligrams(int mg) {
        if (mg < 1000) return mg + "mg";
        int g = Math.round(mg / 1000f);
        return g + "g";
    }

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

        // — Baker’s percentages
        tooltipComponents.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.literal("Baker's Percentages").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));

        // — Flour breakdown
        tooltipComponents.add(Component.literal("Flours:").withStyle(ChatFormatting.GREEN));
        getFlourPercentages(stack).forEach((name, frac) -> {
            int pct = (int) Math.round(frac * 100);
            tooltipComponents.add(Component.literal(String.format(Locale.ROOT, " * %d%% %s", pct, name)).withStyle(ChatFormatting.GREEN));
        });

        // — Other ingredients (percent targets)
        tooltipComponents.add(Component.literal("Other:").withStyle(ChatFormatting.GREEN));
        getOtherIngredientPercentages(stack).forEach((name, pctVal) -> {
            int pct = (int) Math.round(pctVal);
            tooltipComponents.add(Component.literal(String.format(Locale.ROOT, " * %d%% %s", pct, name)).withStyle(ChatFormatting.GREEN));
        });

        // — Actual ingredients (mg-aware, from snapshot)
        var recipe = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (recipe != null && !recipe.ingredients().isEmpty()) {
            tooltipComponents.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));
            tooltipComponents.add(Component.literal("Ingredients (actual)").withStyle(ChatFormatting.GREEN));

            int totalFlourMg = recipe.ingredients().stream()
                    .filter(i -> i.category() == IngredientCategory.FLOUR)
                    .mapToInt(IngredientInfo::milligrams)
                    .sum();

            for (IngredientInfo info : recipe.ingredients()) {
                int mg = info.milligrams();
                double pctOfFlour = (totalFlourMg > 0) ? (mg / (double) totalFlourMg) * 100.0 : 0.0;

                tooltipComponents.add(
                        Component.literal(
                                String.format(Locale.ROOT, "  %s: %s (%.1f%%)",
                                        info.itemId(),
                                        shortWeightLabelFromMilligrams(mg),
                                        pctOfFlour)
                        ).withStyle(ChatFormatting.GRAY)
                );
            }
        }

        // — Footer: Hydration & Weight (mg-aware total)
        tooltipComponents.add(Component.literal("----------------------------------------------").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(
                Component.literal("Hydration: " + (int) Math.round(getHydration(stack)) + "%")
                        .withStyle(ChatFormatting.GREEN)
        );
        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        String weightLabel = (wComp != null)
                ? shortWeightLabelFromGrams(wComp.getWeight())
                : "0g";
        tooltipComponents.add(
                Component.literal("Weight: " + weightLabel)
                        .withStyle(ChatFormatting.GREEN)
        );
    }

    // === Name from enum type =================================================

    @Override
    public Component getName(ItemStack stack) {
        BreadType bt = resolveBreadType(stack);
        if (bt != null) {
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
        if (comp != null) return comp;

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

        int totalFlourMg = recipe.ingredients().stream()
                .filter(i -> i.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::milligrams)
                .sum();
        if (totalFlourMg <= 0) return Collections.emptyMap();

        Map<String, Integer> mgByType = new LinkedHashMap<>();
        for (IngredientInfo info : recipe.ingredients()) {
            if (info.category() != IngredientCategory.FLOUR) continue;

            FlourType ft = info.flourType();
            String key = (ft != null) ? ft.getId() : info.itemId();
            mgByType.merge(key, info.milligrams(), Integer::sum);
        }

        Map<String, Double> pctByType = new LinkedHashMap<>();
        for (var e : mgByType.entrySet()) {
            pctByType.put(titleCaseTokens(e.getKey()),
                    (e.getValue() * 1.0) / totalFlourMg); // fraction 0..1
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
