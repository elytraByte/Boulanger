package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.*;
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
import java.util.Locale;
import java.util.Optional;

public class DoughItem extends Item {
    public DoughItem(Properties properties) {
        super(properties);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static String shortWeightLabelFromGrams(float grams) {
        int mg = Math.max(0, Math.round(grams * 1000f));
        if (mg < 1000) return mg + "mg";
        int g = Math.round(mg / 1000f);
        return g + "g";
    }

    /** Format a per-ingredient weight stored in milligrams (compact). */
    private static String shortWeightLabelFromMilligrams(int mg) {
        if (mg < 1000) return mg + "mg";
        int g = Math.round(mg / 1000f);
        return g + "g";
    }
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        DoughRecipeComponent dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr == null) {
            tooltipComponents.add(Component.literal("Unmixed dough").withStyle(ChatFormatting.RED));
            return;
        }

        // Header
        tooltipComponents.add(Component.literal("Recipe: " + dr.recipeId()).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("-----").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal("Ingredients").withStyle(ChatFormatting.GREEN));

        // Flour total in MG (precise)
        int totalFlourMg = dr.ingredients().stream()
                .filter(info -> info.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::milligrams)
                .sum();

        // Print each ingredient with mg-aware formatting
        for (IngredientInfo info : dr.ingredients()) {
            int mg = info.milligrams();
            double pct = totalFlourMg > 0 ? (mg / (double) totalFlourMg) * 100.0 : 0.0;

            String weightLabel = shortWeightLabelFromMilligrams(mg);

            tooltipComponents.add(
                    Component.literal(
                            String.format(Locale.ROOT, "  %s: %s (%.1f%%)",
                                    info.itemId(), weightLabel, pct)
                    ).withStyle(ChatFormatting.GRAY)
            );
        }

        // Footer
        tooltipComponents.add(Component.literal("-----").withStyle(ChatFormatting.DARK_GRAY));

        // Prefer the stack's float-grams component so totals show mg when <1g
        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        String totalLabel = (wComp != null)
                ? shortWeightLabelFromGrams(wComp.getWeight())
                : (dr.totalWeight() + "g");  // dr.totalWeight() is grams

        tooltipComponents.add(
                Component.literal("Total Weight: " + totalLabel)
                        .withStyle(ChatFormatting.AQUA)
        );

        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (proof != null) {
            tooltipComponents.add(
                    Component.literal(String.format(Locale.ROOT, "Step %d: %d ticks",
                                    proof.stepIndex(), proof.ticksInStep()))
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

        Level level = context.getLevel();
        Optional<DoughProcessRecipe> recipeOpt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(recipeId))
                .findFirst();

        if (recipeOpt.isEmpty()) return InteractionResult.PASS;

        DoughProcessRecipe recipe = recipeOpt.get();
        if (state.stepIndex() >= recipe.getSteps().size()) return InteractionResult.PASS;

        ProcessingStep currentStep = recipe.getSteps().get(state.stepIndex());
        if (currentStep.type() == StepType.PUNCHDOWN) {
            stack.set(ModDataComponentTypes.PROOFING_STATE.get(),
                    new ProofingStateComponent(state.stepIndex() + 1, 0, state.shaped()));
            player.displayClientMessage(Component.literal("Punched down dough!"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return stack.has(ModDataComponentTypes.PROOFING_STATE.get()); }

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
        if (state == null || processId == null || level == null) return 0;

        Optional<DoughProcessRecipe> opt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(processId))
                .findFirst();
        if (opt.isEmpty()) return 0;

        List<ProcessingStep> steps = opt.get().getSteps();
        int idx = state.stepIndex();
        if (idx < 0) return 0;
        if (idx >= steps.size()) return 13; // done

        ProcessingStep step = steps.get(idx);
        float progress = (float) state.ticksInStep() / (float) step.durationTicks();
        return (int) (13f * progress);
    }

    @Override public int getMaxStackSize(ItemStack stack) { return 1; }
    @Override public int getBarColor(ItemStack stack) { return 0x55FFFF; } // light blue
}
