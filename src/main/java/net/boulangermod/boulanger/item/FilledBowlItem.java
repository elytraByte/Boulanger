package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.BakeryAdditiveComponent;
import net.boulangermod.boulanger.component.value.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.value.IngredientItemComponent;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public final class FilledBowlItem extends Item {
    public FilledBowlItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(
                stack,
                context,
                tooltip,
                tooltipFlag
        );

        Component ingredientName = resolveIngredientName(stack);
        if (ingredientName != null) {
            tooltip.add(
                    ingredientName.copy()
                            .withStyle(ChatFormatting.YELLOW)
            );
        }

        WeightComponent weight =
                stack.get(
                        ModDataComponentTypes
                                .INGREDIENT_MILLIGRAMS
                                .get()
                );

        if (weight != null) {
            tooltip.add(
                    Component.literal(
                                    formatWeight(
                                            weight.milligrams()
                                    )
                            )
                            .withStyle(ChatFormatting.GREEN)
            );
        }
    }

    /**
     * Resolves the specific ingredient represented by this bowl.
     *
     * Priority:
     * 1. Flour type, because the source item would otherwise only say "Flour".
     * 2. Original source item.
     * 3. Food additive identity.
     * 4. Bakery additive identity.
     * 5. General ingredient category.
     */
    private static Component resolveIngredientName(
            ItemStack stack
    ) {
        FlourType flourType =
                stack.get(
                        ModDataComponentTypes.FLOUR_TYPE.get()
                );

        if (flourType != null) {
            return Component.literal(
                    beautifyId(flourType.id())
            );
        }

        IngredientItemComponent ingredientItem =
                stack.get(
                        ModDataComponentTypes.INGREDIENT_TYPE.get()
                );

        if (ingredientItem != null
                && ingredientItem.item() != Items.AIR) {
            return new ItemStack(ingredientItem.item())
                    .getHoverName();
        }

        FoodAdditiveComponent foodAdditive =
                stack.get(
                        ModDataComponentTypes.FOOD_ADDITIVE.get()
                );

        if (foodAdditive != null) {
            return Component.literal(
                    beautifyId(foodAdditive.id())
            );
        }

        BakeryAdditiveComponent bakeryAdditive =
                stack.get(
                        ModDataComponentTypes.BAKERY_ADDITIVE.get()
                );

        if (bakeryAdditive != null) {
            return Component.literal(
                    beautifyId(bakeryAdditive.id())
            );
        }

        IngredientCategory category =
                stack.get(
                        ModDataComponentTypes
                                .INGREDIENT_CATEGORY
                                .get()
                );

        if (category != null) {
            return Component.literal(
                    toTitleCase(
                            category.name()
                                    .replace('_', ' ')
                                    .toLowerCase(Locale.ROOT)
                    )
            );
        }

        return null;
    }

    /**
     * Formats milligrams without floating-point arithmetic.
     */
    private static String formatWeight(
            long milligrams
    ) {
        milligrams = Math.max(0L, milligrams);

        if (milligrams < 1_000L) {
            return "Weight: " + milligrams + " mg";
        }

        long wholeGrams = milligrams / 1_000L;
        long remainingMilligrams = milligrams % 1_000L;

        if (remainingMilligrams == 0L) {
            return "Weight: " + wholeGrams + " g";
        }

        String fraction = String.format(
                Locale.ROOT,
                "%03d",
                remainingMilligrams
        );

        int end = fraction.length();
        while (end > 0
                && fraction.charAt(end - 1) == '0') {
            end--;
        }

        return "Weight: "
                + wholeGrams
                + "."
                + fraction.substring(0, end)
                + " g";
    }

    private static String beautifyId(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }

        String path = id;
        int namespaceSeparator = id.indexOf(':');

        if (namespaceSeparator >= 0
                && namespaceSeparator + 1 < id.length()) {
            path = id.substring(namespaceSeparator + 1);
        }

        return toTitleCase(
                path.replace('_', ' ')
                        .toLowerCase(Locale.ROOT)
        );
    }

    private static String toTitleCase(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String[] words = value.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(word.charAt(0))
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }
}