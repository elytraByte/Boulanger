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

    // ── NEW helpers ───────────────────────────────────────────────────────────
    private static String shortWeightLabelFromGrams(float grams) {
        int mg = Math.max(0, Math.round(grams * 1000f));
        if (mg < 1000) return mg + " mg";
        return String.format(java.util.Locale.ROOT, "%.3f g", mg / 1000.0);
    }

    private static String shortWeightLabelFromWholeGramsOrLess(int grams, double pctOfFlour) {
        // If recipe only stored whole grams and says 0g, but pct>0, show "<1 g"
        if (grams <= 0 && pctOfFlour > 0.0) return "<1 g";
        return grams + " g";
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

        // 1) Flour total in stored units (whole grams in your current component)
        int flourTotalG = dr.ingredients().stream()
                .filter(info -> info.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::weight)
                .sum();

        // 2) Print each ingredient
        for (IngredientInfo info : dr.ingredients()) {
            int wG = info.weight(); // current stored unit = whole grams
            double pct = flourTotalG > 0 ? (double) wG / flourTotalG * 100.0 : 0.0;

            // If you later add mg to IngredientInfo, prefer that here and format via mg.
            String weightLabel = shortWeightLabelFromWholeGramsOrLess(wG, pct);

            tooltipComponents.add(
                    Component.literal(
                            String.format("  %s: %s (%.1f%%)", info.itemId(), weightLabel, pct)
                    ).withStyle(ChatFormatting.GRAY)
            );
        }

        // Footer
        tooltipComponents.add(Component.literal("-----").withStyle(ChatFormatting.DARK_GRAY));

        // Prefer the stack's weight component (float grams) so sub-gram totals show as mg
        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        String totalLabel = (wComp != null)
                ? shortWeightLabelFromGrams(wComp.getWeight())
                : (dr.totalWeight() + " g");

        tooltipComponents.add(
                Component.literal("Total Weight: " + totalLabel)
                        .withStyle(ChatFormatting.AQUA)
        );

        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (proof != null) {
            tooltipComponents.add(
                    Component.literal(String.format("Step %d: %d ticks", proof.stepIndex(), proof.ticksInStep()))
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
