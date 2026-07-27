package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.BakeryAdditiveComponent;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.additive.BakeryAdditiveType;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class BakeryAdditiveItem extends Item {
    private final BakeryAdditiveType type;

    public BakeryAdditiveItem(
            Properties properties,
            BakeryAdditiveType type
    ) {
        super(configureProperties(properties, type));
        this.type = type;
    }

    private static Properties configureProperties(
            Properties properties,
            BakeryAdditiveType type
    ) {
        properties
                .component(
                        ModDataComponentTypes
                                .BAKERY_ADDITIVE
                                .get(),
                        type.toComponent()
                )
                .component(
                        ModDataComponentTypes
                                .INGREDIENT_CATEGORY
                                .get(),
                        type.category()
                );

        /*
         * These are mutable-weight bulk packages. Giving them
         * their full weight as a default component causes
         * IngredientMassResolver to use VARIABLE_WEIGHT from
         * the first measurement onward.
         */
        if (isBulkConditioner(type)) {
            properties.component(
                    ModDataComponentTypes
                            .INGREDIENT_MILLIGRAMS
                            .get(),
                    WeightComponent.ofMilligrams(
                            type.unitMg()
                    )
            );
        }

        return properties;
    }

    public BakeryAdditiveType getType() {
        return type;
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

        BakeryAdditiveComponent additive =
                stack.get(
                        ModDataComponentTypes
                                .BAKERY_ADDITIVE
                                .get()
                );

        String id = additive != null
                ? additive.id()
                : type.id();

        IngredientCategory category =
                stack.get(
                        ModDataComponentTypes
                                .INGREDIENT_CATEGORY
                                .get()
                );

        if (category == null) {
            category = type.category();
        }

        tooltip.add(
                Component.literal(
                                "Additive: "
                                        + titleCaseTokens(id)
                        )
                        .withStyle(ChatFormatting.GREEN)
        );

        tooltip.add(
                Component.literal(
                                "----------------------------------------------"
                        )
                        .withStyle(ChatFormatting.GREEN)
        );

        tooltip.add(
                Component.literal(
                                "Category: "
                                        + titleCaseTokens(
                                        category.name()
                                )
                        )
                        .withStyle(ChatFormatting.DARK_GRAY)
        );

        if (isBulkConditioner(type)) {
            tooltip.add(
                    Component.literal(
                                    "Weight: "
                                            + formatMilligrams(
                                            currentMilligrams(stack)
                                    )
                                            + " / "
                                            + formatMilligrams(
                                            type.unitMg()
                                    )
                            )
                            .withStyle(ChatFormatting.GREEN)
            );
        } else {
            WeightComponent explicitWeight =
                    stack.get(
                            ModDataComponentTypes
                                    .INGREDIENT_MILLIGRAMS
                                    .get()
                    );

            long totalMilligrams =
                    explicitWeight != null
                            ? explicitWeight.milligrams()
                            : Math.multiplyExact(
                            type.unitMg(),
                            (long) stack.getCount()
                    );

            tooltip.add(
                    Component.literal(
                                    "Weight: "
                                            + formatMilligrams(
                                            totalMilligrams
                                    )
                            )
                            .withStyle(ChatFormatting.GREEN)
            );
        }
    }

    /*
     * These three boxes always display their bar, including
     * when they are completely full.
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return isBulkConditioner(type);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long capacityMilligrams =
                Math.max(1L, type.unitMg());

        long currentMilligrams = Math.max(
                0L,
                Math.min(
                        currentMilligrams(stack),
                        capacityMilligrams
                )
        );

        return (int) Math.round(
                13.0
                        * currentMilligrams
                        / capacityMilligrams
        );
    }

    @Override
    public int getBarColor(ItemStack stack) {
        long capacityMilligrams =
                Math.max(1L, type.unitMg());

        long currentMilligrams = Math.max(
                0L,
                Math.min(
                        currentMilligrams(stack),
                        capacityMilligrams
                )
        );

        double percentage =
                (double) currentMilligrams
                        / capacityMilligrams;

        int red = (int) Math.round(
                (1.0 - percentage) * 255.0
        );

        int green = (int) Math.round(
                percentage * 255.0
        );

        return (red << 16) | (green << 8);
    }

    private long currentMilligrams(ItemStack stack) {
        WeightComponent weight =
                stack.get(
                        ModDataComponentTypes
                                .INGREDIENT_MILLIGRAMS
                                .get()
                );

        return weight != null
                ? weight.milligrams()
                : type.unitMg();
    }

    private static boolean isBulkConditioner(
            BakeryAdditiveType type
    ) {
        return switch (type) {
            case S_500_RED,
                 IM_PROVE_200,
                 ADVANTAGE_500_CL -> true;

            default -> false;
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        BakeryAdditiveComponent additive =
                stack.get(
                        ModDataComponentTypes
                                .BAKERY_ADDITIVE
                                .get()
                );

        String id = additive != null
                ? additive.id()
                : type.id();

        return Component.translatable(
                "item.boulanger.bakery_additive." + id
        );
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

    private static String titleCaseTokens(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String key = raw.contains(":")
                ? raw.substring(raw.indexOf(':') + 1)
                : raw;

        String[] parts = key
                .toLowerCase(Locale.ROOT)
                .split("_");

        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(
                            part.charAt(0)
                    )
            );

            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }

        return result.toString();
    }
}