package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientTypeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class FilledBowlItem extends Item {
    public FilledBowlItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        // ── Ingredient name (beautified) ───────────────────────────────────────
        String ingredientName = resolveIngredientName(stack);
        if (ingredientName != null && !ingredientName.isEmpty()) {
            tooltip.add(Component.literal(ingredientName).withStyle(ChatFormatting.YELLOW));
        }

        // ── Weight line (green) ────────────────────────────────────────────────
        WeightComponent weightComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (weightComp != null) {
            tooltip.add(Component.literal(formatWeight(weightComp.grams()))
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    /**
     * Resolve a friendly ingredient name for the tooltip.
     * Priority:
     *   1) If FlourType is present → use its id (beautified) to avoid generic "Flour".
     *   2) Else IngredientTypeComponent (the item’s localized display name).
     *   3) Else IngredientCategory (beautified enum).
     */
    private static String resolveIngredientName(ItemStack stack) {
        // 1) Flour type first (so flour shows "Bread Flour", etc.)
        FlourType flourType = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (flourType != null) {
            return beautifyId(flourType.getId());
        }

        // 2) Specific item via IngredientTypeComponent (uses item’s localized name)
        IngredientTypeComponent itc = stack.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (itc != null) {
            Item item = itc.item();
            if (item != null) {
                return new ItemStack(item).getHoverName().getString();
            }
        }

        // 3) Category fallback (beautified enum)
        IngredientCategory category = stack.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
        if (category != null) {
            return toTitleCase(category.name().replace('_', ' ').toLowerCase(Locale.ROOT));
        }

        return null;
    }

    private static String formatWeight(float grams) {
        if (grams < 1f) {
            int mg = Math.round(grams * 1000f);
            return String.format(Locale.ROOT, "Weight: %d mg", mg);
        }
        // If effectively an integer, show without decimals
        if (Math.abs(grams - Math.round(grams)) < 0.0005f) {
            return "Weight: " + Math.round(grams) + " g";
        }
        return String.format(Locale.ROOT, "Weight: %.3f g", grams);
    }

    /** Accepts ids with or without namespace; uses the path part if present. */
    private static String beautifyId(String id) {
        if (id == null || id.isEmpty()) return "";
        String path = id;
        int colon = id.indexOf(':');
        if (colon >= 0 && colon + 1 < id.length()) {
            path = id.substring(colon + 1);
        }
        path = path.replace('_', ' ').toLowerCase(Locale.ROOT);
        return toTitleCase(path);
    }

    private static String toTitleCase(String s) {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                out.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) out.append(p.substring(1));
            }
            if (i + 1 < parts.length) out.append(' ');
        }
        return out.toString();
    }
}
