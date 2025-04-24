package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
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
        tooltipComponents.add(
                Component.literal("----------------------------------------------")
                        .withStyle(ChatFormatting.GREEN)
        );
        tooltipComponents.add(
                Component.literal("Baker's Percentages")
                        .withStyle(ChatFormatting.GREEN)
        );
        tooltipComponents.add(
                Component.literal("----------------------------------------------")
                        .withStyle(ChatFormatting.GREEN)
        );

        // — Flour breakdown
        tooltipComponents.add(
                Component.literal("Flours:")
                        .withStyle(ChatFormatting.GREEN)
        );
        getFlourPercentages(stack).forEach((name, frac) -> {
            int pct = (int) Math.round(frac * 100);
            tooltipComponents.add(
                    Component.literal(String.format(" * %d%% %s", pct, name))
                            .withStyle(ChatFormatting.GREEN)
            );
        });

        // — Other ingredients (percent values already in pctComp)
        tooltipComponents.add(
                Component.literal("Other:")
                        .withStyle(ChatFormatting.GREEN)
        );
        getOtherIngredientPercentages(stack).forEach((name, pctVal) -> {
            int pct = (int) Math.round(pctVal);
            tooltipComponents.add(
                    Component.literal(String.format(" * %d%% %s", pct, name))
                            .withStyle(ChatFormatting.GREEN)
            );
        });

        // — Footer: Hydration & Weight
        tooltipComponents.add(
                Component.literal("----------------------------------------------")
                        .withStyle(ChatFormatting.GREEN)
        );
        tooltipComponents.add(
                Component.literal(
                        "Hydration: " + (int)Math.round(getHydration(stack)) + "% (this is the water)"
                ).withStyle(ChatFormatting.GREEN)
        );
        tooltipComponents.add(
                Component.literal("Weight: " + getTotalWeight(stack) + " g")
                        .withStyle(ChatFormatting.GREEN)
        );
    }

    // — Helpers —

    private String getTypeName(ItemStack stack) {
        var type = stack.get(ModDataComponentTypes.BREAD_TYPE.get());
        return (type != null)
                ? type.name().toLowerCase(Locale.ROOT)
                : "unknown";
    }

    private Map<String, Double> getFlourPercentages(ItemStack stack) {
        var recipe = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (recipe == null) return Collections.emptyMap();

        double totalFlour = recipe.ingredients().stream()
                .filter(i -> i.category() == IngredientCategory.FLOUR)
                .mapToDouble(IngredientInfo::weight)
                .sum();
        if (totalFlour <= 0) return Collections.emptyMap();

        Map<String, Double> map = new LinkedHashMap<>();
        for (IngredientInfo info : recipe.ingredients()) {
            if (info.category() == IngredientCategory.FLOUR) {
                map.put(info.itemId(), info.weight() / totalFlour);
            }
        }
        return map;
    }

    private Map<String, Double> getOtherIngredientPercentages(ItemStack stack) {
        var pctComp = stack.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        if (pctComp == null) return Collections.emptyMap();

        Map<String, Double> map = new LinkedHashMap<>();
        for (Map.Entry<IngredientCategory, Double> e : pctComp.percentages().entrySet()) {
            if (e.getKey() != IngredientCategory.FLOUR) {
                map.put(
                        e.getKey().name().toLowerCase(Locale.ROOT),
                        e.getValue()
                );
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
}
