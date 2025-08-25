package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.util.IngredientCategory;
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
        super(properties);
        this.type = type;
    }

    public BakeryAdditiveType getType() { return type; }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), type.getCategory());
        stack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(type.getWeight()));
        stack.set(ModDataComponentTypes.INGREDIENT_TYPE.get(), new IngredientTypeComponent(this));
        // The one structured identity you have:
        stack.set(ModDataComponentTypes.FOOD_ADDITIVE.get(), type.toFoodAdditiveComponent());
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        FoodAdditiveComponent add = stack.get(ModDataComponentTypes.FOOD_ADDITIVE.get());
        float defaultG = (add != null) ? add.getWeight() : type.getWeight();

        WeightComponent wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        float weightToDisplay = (wComp != null) ? wComp.getWeight() : defaultG;

        IngredientCategory cat = stack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        if (cat == null) cat = type.getCategory();

        tooltip.add(Component.literal("Additive: " + titleCaseTokens(type.getId()))
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("----------------------------------------------")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Category: " + titleCaseTokens(cat.name()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Weight: " + Math.round(weightToDisplay) + " g")
                .withStyle(ChatFormatting.GREEN));
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
