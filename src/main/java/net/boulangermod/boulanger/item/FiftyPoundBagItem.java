package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.List;

public class FiftyPoundBagItem extends Item {
    public static final float MAX_GRAMS = 22680f;

    public FiftyPoundBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        WeightComponent weight = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        float grams = weight != null ? weight.grams() : 0f;
        return Math.round(13f * (grams / MAX_GRAMS));
    }


    @Override
    public int getBarColor(ItemStack stack) {
        WeightComponent weight = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (weight == null) return 0xFF0000; // solid red fallback

        float percent = weight.grams() / MAX_GRAMS;
        int red = (int) ((1.0f - percent) * 255);
        int green = (int) (percent * 255);
        return (red << 16) | (green << 8);
    }



    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        FlourType flour = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        WeightComponent weight = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        float grams = (weight != null) ? weight.grams() : (flour != null ? flour.weight() : 0f);

        if (flour != null) {
            // Translatable flour name from lang: "item.boulanger.flour.ap": "All Purpose"
            String flourKey = "item.boulanger.flour." + ResourceLocation.tryParse(flour.type()).getPath();

            tooltipComponents.add(Component.literal("Contains: ")
                    .append(Component.translatable(flourKey))
                    .withStyle(ChatFormatting.GRAY));

            tooltipComponents.add(Component.literal("Ash: " + flour.ash() + "%").withStyle(ChatFormatting.DARK_GRAY));
            tooltipComponents.add(Component.literal("Protein: " + flour.protein() + "%").withStyle(ChatFormatting.BLUE));
        }

        tooltipComponents.add(Component.literal(String.format("Weight: %.1fg / %.1fg", grams, MAX_GRAMS))
                .withStyle(ChatFormatting.GREEN));
    }


    private String toTitleCase(String raw) {
        String[] words = raw.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    public FlourType getFlourType(ItemStack stack) {
        return stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
    }

}