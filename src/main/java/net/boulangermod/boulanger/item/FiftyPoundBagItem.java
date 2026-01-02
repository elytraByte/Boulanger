package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.WeightComponent;
import net.boulangermod.boulanger.content.flour.FiftyPoundBagType;
import net.boulangermod.boulanger.content.flour.FlourType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FiftyPoundBagItem extends Item {

    public static final int MAX_MILLIGRAMS = FiftyPoundBagType.MAX_MILLIGRAMS;

    public FiftyPoundBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        FlourType flour = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        if (flour == null) return super.getDescriptionId(stack);

        return "item." + Boulanger.MOD_ID + ".fifty_pound_bag." + flourPath(flour);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        WeightComponent weight = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
        int mg = (weight != null) ? weight.milligrams() : 0;
        return Math.round(13f * (mg / (float) MAX_MILLIGRAMS));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        WeightComponent weight = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());
        if (weight == null) return 0xFF0000;

        float percent = weight.milligrams() / (float) MAX_MILLIGRAMS;
        percent = Math.max(0f, Math.min(1f, percent));

        int red = (int) ((1.0f - percent) * 255f);
        int green = (int) (percent * 255f);
        return (red << 16) | (green << 8);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        FlourType flour = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        WeightComponent weight = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());

        int mg = (weight != null) ? weight.milligrams() : (flour != null ? flour.unitMg() : 0);

        if (flour != null) {
            String flourKey = "item." + Boulanger.MOD_ID + ".flour." + flourPath(flour);

            tooltip.add(Component.literal("Contains: ")
                    .append(Component.translatable(flourKey))
                    .withStyle(ChatFormatting.GRAY));

            tooltip.add(Component.literal(String.format("Ash: %.2f%%", flour.ash()))
                    .withStyle(ChatFormatting.DARK_GRAY));

            tooltip.add(Component.literal(String.format("Protein: %.1f%%", flour.protein()))
                    .withStyle(ChatFormatting.BLUE));
        }

        tooltip.add(Component.literal(String.format(
                        "Weight: %.1fg / %.1fg",
                        mg / 1000f,
                        MAX_MILLIGRAMS / 1000f
                ))
                .withStyle(ChatFormatting.GREEN));
    }

    private static String flourPath(FlourType flour) {
        ResourceLocation rl = ResourceLocation.tryParse(flour.id());
        return (rl != null) ? rl.getPath() : flour.id();
    }
}
