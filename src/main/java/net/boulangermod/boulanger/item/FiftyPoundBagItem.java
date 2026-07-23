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

    public static final long MAX_MILLIGRAMS = FiftyPoundBagType.MAX_MILLIGRAMS;

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
        WeightComponent weight =
                stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());

        long mg = weight != null ? weight.milligrams() : 0L;
        long clampedMg = Math.max(0L, Math.min(mg, MAX_MILLIGRAMS));

        return (int) Math.round(
                13.0 * clampedMg / MAX_MILLIGRAMS
        );
    }

    @Override
    public int getBarColor(ItemStack stack) {
        WeightComponent weight =
                stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());

        if (weight == null) {
            return 0xFF0000;
        }

        long mg = Math.max(
                0L,
                Math.min(weight.milligrams(), MAX_MILLIGRAMS)
        );

        double percent = (double) mg / MAX_MILLIGRAMS;

        int red = (int) Math.round((1.0 - percent) * 255.0);
        int green = (int) Math.round(percent * 255.0);

        return (red << 16) | (green << 8);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        FlourType flour = stack.get(ModDataComponentTypes.FLOUR_TYPE.get());
        WeightComponent weight = stack.get(ModDataComponentTypes.INGREDIENT_MILLIGRAMS.get());

        long mg = (weight != null) ? weight.milligrams() : (flour != null ? flour.unitMg() : 0);

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
