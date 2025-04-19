package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.BakerPctComponent;
import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Map;

public class BreadItem extends Item {
    public BreadItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // pull out all of your components
        BreadType type        = stack.get(ModDataComponentTypes.BREAD_TYPE.get());
        WeightComponent wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        BakerPctComponent pctComp   = stack.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        DoughRecipeComponent recipeComp = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());

        if (type != null) {
            // — Type
            tooltipComponents.add(
                    Component.literal("Type: " + type.name())
                            .withStyle(ChatFormatting.GRAY)
            );

            // — Total weight
            if (wComp != null) {
                tooltipComponents.add(
                        Component.literal(String.format("Weight: %.1f g", wComp.getWeight()))
                                .withStyle(ChatFormatting.GREEN)
                );
            } else {
                tooltipComponents.add(
                        Component.literal("Weight: unknown")
                                .withStyle(ChatFormatting.DARK_GRAY)
                );
            }

            // — Baker's percentages
            if (pctComp != null && recipeComp != null) {
                // 1) Total flour weight
                double flourWeight = 0;
                for (IngredientInfo info : recipeComp.ingredients()) {
                    if (info.category() == IngredientCategory.FLOUR) {
                        flourWeight += info.weight();
                    }
                }

                tooltipComponents.add(
                        Component.literal("Baker’s %:")
                                .withStyle(ChatFormatting.YELLOW)
                );

                // 2) Flour breakdown
                if (flourWeight > 0) {
                    tooltipComponents.add(
                            Component.literal("  Flours:")
                                    .withStyle(ChatFormatting.GRAY)
                    );
                    for (IngredientInfo info : recipeComp.ingredients()) {
                        if (info.category() == IngredientCategory.FLOUR) {
                            double pct = info.weight() / flourWeight * 100.0;
                            tooltipComponents.add(
                                    Component.literal(
                                            String.format("    %s: %.1f%%", info.itemId(), pct)
                                    ).withStyle(ChatFormatting.GRAY)
                            );
                        }
                    }
                }

                // 3) Other categories from your pctComp map
                tooltipComponents.add(
                        Component.literal("  Others:")
                                .withStyle(ChatFormatting.GRAY)
                );
                for (Map.Entry<IngredientCategory, Double> entry : pctComp.percentages().entrySet()) {
                    if (entry.getKey() != IngredientCategory.FLOUR) {
                        String catName = entry.getKey().name().toLowerCase();
                        double pct     = entry.getValue();
                        tooltipComponents.add(
                                Component.literal(
                                        String.format("    %s: %.1f%%", catName, pct)
                                ).withStyle(ChatFormatting.GRAY)
                        );
                    }
                }
            }

            // — Full ingredient list with grams
            if (recipeComp != null) {
                tooltipComponents.add(
                        Component.literal("Ingredients:")
                                .withStyle(ChatFormatting.GOLD)
                );
                for (IngredientInfo info : recipeComp.ingredients()) {
                    tooltipComponents.add(
                            Component.literal(
                                    String.format("  %s: %dg", info.itemId(), info.weight())
                            ).withStyle(ChatFormatting.GRAY)
                    );
                }
            }

        } else {
            tooltipComponents.add(
                    Component.literal("Unbaked dough?")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
