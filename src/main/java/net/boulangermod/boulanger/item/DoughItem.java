package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;


import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class DoughItem extends Item {
    public DoughItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {

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
                Component.literal("Recipe: " + dr.recipeId().toString())
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

        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (proof != null) {
            tooltipComponents.add(Component.literal(
                            String.format("Step %d: %d ticks", proof.stepIndex(), proof.ticksInStep()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
            );
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();

        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation recipeId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (state == null || recipeId == null) return InteractionResult.PASS;

        var level = context.getLevel();
        var recipeOpt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(recipeId))
                .findFirst();

        if (recipeOpt.isEmpty()) return InteractionResult.PASS;

        DoughProcessRecipe recipe = recipeOpt.get();
        if (state.stepIndex() >= recipe.getSteps().size()) return InteractionResult.PASS;

        var currentStep = recipe.getSteps().get(state.stepIndex());

        if (currentStep.type() == StepType.PUNCHDOWN) {
            stack.set(ModDataComponentTypes.PROOFING_STATE.get(),
                    new ProofingStateComponent(state.stepIndex() + 1, 0, state.shaped()));
            player.displayClientMessage(Component.literal("Punched down dough!"), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponentTypes.PROOFING_STATE.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return 0;
        return getBarWidth(stack, level);
    }

    // Internal helper — no @Override
    public int getBarWidth(ItemStack stack, @Nullable Level level) {
        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation processId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (state == null || processId == null || level == null) {
            return 0;
        }

        Optional<DoughProcessRecipe> opt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(processId))
                .findFirst();

        if (opt.isEmpty()) {
            return 0;
        }

        List<ProcessingStep> steps = opt.get().getSteps();
        int idx = state.stepIndex();

        if (idx < 0) {
            return 0;
        } else if (idx >= steps.size()) {
            // fully done → full bar
            return 13;
        }

        ProcessingStep step = steps.get(idx);
        float progress = (float) state.ticksInStep() / (float) step.durationTicks();
        return (int) (13f * progress);
    }

    // Force dough items to never stack above 1
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    // Light-blue proofing bar (RGB 0x55FFFF)
    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55FFFF;
    }
}
