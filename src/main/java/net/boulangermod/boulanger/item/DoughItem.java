package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class DoughItem extends Item {
    public DoughItem(Properties properties) {
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

        DoughRecipeComponent dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr == null) {
            tooltipComponents.add(
                    Component.literal("Unmixed dough")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        // Header
        tooltipComponents.add(
                Component.literal("Recipe: " + dr.recipeName())
                        .withStyle(ChatFormatting.GOLD)
        );
        tooltipComponents.add(
                Component.literal("-----")
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
        tooltipComponents.add(
                Component.literal("Ingredients")
                        .withStyle(ChatFormatting.GREEN)
        );

        // 1) Compute total flour weight (for baker's % denominator)
        int flourTotal = dr.ingredients().stream()
                .filter(info -> info.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::weight)
                .sum();

        // 2) Iterate every ingredient and print "Name: Xg (Y.Y%)"
        for (IngredientInfo info : dr.ingredients()) {
            int w = info.weight();
            double pct = flourTotal > 0
                    ? (double) w / flourTotal * 100.0
                    : 0.0;

            // e.g. "  whole_wheat_flour: 200g (100.0%)"
            tooltipComponents.add(
                    Component.literal(
                            String.format("  %s: %dg (%.1f%%)",
                                    info.itemId(),
                                    w,
                                    pct)
                    ).withStyle(ChatFormatting.GRAY)
            );
        }

        // Footer
        tooltipComponents.add(
                Component.literal("-----")
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
        tooltipComponents.add(
                Component.literal(String.format("Total Weight: %dg", dr.totalWeight()))
                        .withStyle(ChatFormatting.AQUA)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());

        if (state != null && state.proofed()) {
            int punches = state.punchCount();
            // Optionally check against recipe max punches
            stack.set(ModDataComponentTypes.PROOFING_STATE.get(),
                    new ProofingStateComponent(true, punches + 1));
            player.displayClientMessage(Component.literal("Punched down dough!"), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

}
