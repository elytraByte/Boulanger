package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

public class DoughItem extends Item {
    public DoughItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // Fetch the DoughRecipeComponent
        DoughRecipeComponent dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr != null) {
            // Recipe name
            tooltipComponents.add(Component.literal("Recipe: " + dr.recipeName())
                    .withStyle(ChatFormatting.GOLD));

            // Baker's percentages
            tooltipComponents.add(Component.literal("Baker’s %:")
                    .withStyle(ChatFormatting.GRAY));
            for (Map.Entry<net.boulangermod.boulanger.util.IngredientCategory, Double> e
                    : dr.targetPercentages().entrySet()) {
                String cat = e.getKey().name().toLowerCase();
                String pct = String.format("%.1f%%", e.getValue());
                tooltipComponents.add(Component.literal("  " + cat + ": " + pct)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            // Ingredients & weights
            tooltipComponents.add(Component.literal("Ingredients:")
                    .withStyle(ChatFormatting.GRAY));
            for (IngredientInfo info : dr.ingredients()) {
                tooltipComponents.add(Component.literal(
                                String.format("  %s: %dg",
                                        info.itemId(), info.weight()))
                        .withStyle(ChatFormatting.GREEN));
            }

            // Total weight
            tooltipComponents.add(Component.literal("Total weight: " + dr.totalWeight() + "g")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltipComponents.add(Component.literal("Unmixed dough")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
