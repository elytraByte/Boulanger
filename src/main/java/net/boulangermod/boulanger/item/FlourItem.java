package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class FlourItem extends Item {
    public FlourItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (type == null) {
            tooltip.add(Component.literal("No flour data").withStyle(ChatFormatting.RED));
            return;
        }

        String idPath = pathOnly(type.id());
        String prettyName = pretty(idPath);
        String label;
        if (prettyName.endsWith(" Flour")) {
            label = "Flour Type: ";
            prettyName = prettyName.substring(0, prettyName.length() - 6);
        } else {
            label = "Type: ";
        }

        tooltip.add(
                Component.literal(label + prettyName)
                        .withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "Ash: %.2f%%",
                                        type.ash()
                                )
                        )
                        .withStyle(ChatFormatting.DARK_GRAY)
        );

        tooltip.add(
                Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "Protein: %.1f%%",
                                        type.protein()
                                )
                        )
                        .withStyle(ChatFormatting.BLUE)
        );

        WeightComponent explicitWeight = stack.get(
                ModDataComponentTypes
                        .INGREDIENT_MILLIGRAMS
                        .get()
        );

        long unitMilligrams = type.unitMg();

        if (explicitWeight != null) {
            long currentMilligrams =
                    explicitWeight.milligrams();

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
        } else {
            tooltip.add(
                    Component.literal(
                                    "Unit: "
                                            + formatMilligrams(
                                            unitMilligrams
                                    )
                            )
                            .withStyle(ChatFormatting.GREEN)
            );

            if (stack.getCount() > 1) {
                long totalMilligrams =
                        type.totalMilligrams(stack);

                tooltip.add(
                        Component.literal(
                                        "Total: "
                                                + formatMilligrams(
                                                totalMilligrams
                                        )
                                )
                                .withStyle(ChatFormatting.GREEN)
                );
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        FlourType type = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (type == null) return super.getName(stack);

        String idPath = pathOnly(type.id());
        return Component.translatable("item.boulanger.flour." + idPath);
    }

    private static String pathOnly(String maybeNamespaced) {
        ResourceLocation rl = ResourceLocation.tryParse(maybeNamespaced);
        return rl != null ? rl.getPath() : maybeNamespaced;
    }

    private static String pretty(String idPath) {
        String[] parts = idPath.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
        }
        return sb.toString();
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
}