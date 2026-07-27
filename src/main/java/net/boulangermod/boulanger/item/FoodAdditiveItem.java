package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.additive.FoodAdditiveType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class FoodAdditiveItem extends Item {
    private final FoodAdditiveType additiveType;

    public FoodAdditiveItem(
            Properties properties,
            FoodAdditiveType type
    ) {
        super(
                properties
                        .component(
                                ModDataComponentTypes
                                        .FOOD_ADDITIVE
                                        .get(),
                                type.toComponent()
                        )
                        .component(
                                ModDataComponentTypes
                                        .INGREDIENT_CATEGORY
                                        .get(),
                                type.category()
                        )
        );

        this.additiveType = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(
                stack,
                context,
                tooltip,
                flag
        );

        FoodAdditiveComponent component =
                stack.get(
                        ModDataComponentTypes
                                .FOOD_ADDITIVE
                                .get()
                );

        String id = component != null
                ? component.id()
                : additiveType.id();

        tooltip.add(
                Component.literal(
                                "Additive: "
                                        + pretty(id)
                        )
                        .withStyle(ChatFormatting.GRAY)
        );

        WeightComponent explicitWeight =
                stack.get(
                        ModDataComponentTypes
                                .INGREDIENT_MILLIGRAMS
                                .get()
                );

        long unitMilligrams =
                additiveType.unitMg();

        /*
         * A normally stackable, untouched ingredient has no
         * explicit weight component. Show both its unit weight
         * and the total represented by the complete stack.
         */
        if (explicitWeight == null
                && stack.getMaxStackSize() > 1) {
            long stackMilligrams =
                    Math.multiplyExact(
                            unitMilligrams,
                            (long) stack.getCount()
                    );

            tooltip.add(
                    Component.literal(
                                    "Unit Weight: "
                                            + formatMilligrams(
                                            unitMilligrams
                                    )
                            )
                            .withStyle(ChatFormatting.GREEN)
            );

            tooltip.add(
                    Component.literal(
                                    "Stack Weight: "
                                            + formatMilligrams(
                                            stackMilligrams
                                    )
                                            + " ("
                                            + stack.getCount()
                                            + " units)"
                            )
                            .withStyle(ChatFormatting.GREEN)
            );

            return;
        }

        /*
         * Explicitly weighed partial items and naturally
         * non-stackable items display their current weight
         * relative to their original unit size.
         */
        long currentMilligrams =
                explicitWeight != null
                        ? explicitWeight.milligrams()
                        : unitMilligrams;

        tooltip.add(
                Component.literal(
                                "Weight: "
                                        + formatMilligrams(
                                        currentMilligrams
                                )
                                        + " / "
                                        + formatMilligrams(
                                        unitMilligrams
                                )
                        )
                        .withStyle(ChatFormatting.GREEN)
        );
    }

    @Override
    public Component getName(ItemStack stack) {
        FoodAdditiveComponent component =
                stack.get(
                        ModDataComponentTypes
                                .FOOD_ADDITIVE
                                .get()
                );

        String id = component != null
                ? component.id()
                : additiveType.id();

        return Component.translatable(
                "item.boulanger.food_additive." + id
        );
    }

    public FoodAdditiveType getType() {
        return additiveType;
    }

    private static String formatMilligrams(
            long milligrams
    ) {
        milligrams = Math.max(0L, milligrams);

        if (milligrams < 1_000L) {
            return milligrams + " mg";
        }

        long grams = milligrams / 1_000L;
        long remainder = milligrams % 1_000L;

        if (remainder == 0L) {
            return grams + " g";
        }

        String fraction = String.format(
                Locale.ROOT,
                "%03d",
                remainder
        );

        int end = fraction.length();

        while (end > 0
                && fraction.charAt(end - 1) == '0') {
            end--;
        }

        return grams
                + "."
                + fraction.substring(0, end)
                + " g";
    }

    private static String pretty(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }

        int separator = id.indexOf(':');

        String path = separator >= 0
                ? id.substring(separator + 1)
                : id;

        String[] words = path
                .toLowerCase(Locale.ROOT)
                .split("_");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(
                            word.charAt(0)
                    )
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }
}