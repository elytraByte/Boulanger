package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

public class DoughItem extends Item {
    public DoughItem(Properties properties) { super(properties); }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static String shortWeightLabelFromGrams(float grams) {
        int mg = Math.max(0, Math.round(grams * 1000f));
        if (mg < 1000) return mg + "mg";
        int g = Math.round(mg / 1000f);
        return g + "g";
    }

    private static String titleCaseTokens(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String key = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)))
                        .append(p.length() > 1 ? p.substring(1) : "");
                if (i < parts.length - 1) sb.append(' ');
            }
        }
        return sb.toString();
    }
    // ─────────────────────────────────────────────────────────────────────────

    // === Name line: "<Recipe> Dough (450g)" ================================
    @Override
    public Component getName(ItemStack stack) {
        // Prefer the recipeId (from saved snapshot), else process id, else fallback
        String base = "Dough";
        DoughRecipeComponent dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr != null && dr.recipeId() != null) {
            base = titleCaseTokens(dr.recipeId().getPath()) + " Dough";
        } else {
            ResourceLocation proc = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
            if (proc != null) base = titleCaseTokens(proc.getPath()) + " Dough";
        }

        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (wComp != null && wComp.getWeight() > 0f) {
            String weight = shortWeightLabelFromGrams(wComp.getWeight());
            return Component.literal(base + " (" + weight + ")");
        }
        return Component.literal(base);
    }

    // === Tooltip: minimal by default; Shift → details ======================
    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag tooltipFlag) {

        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        DoughRecipeComponent dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr == null) {
            tooltip.add(Component.literal("Unmixed dough").withStyle(ChatFormatting.RED));
            return;
        }

        // ── Proofing state: always shown (1-based index + friendly step name) ──
        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation procId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (proof != null) {
            int idx0 = Math.max(0, proof.stepIndex()); // internal 0-based
            int idx1 = idx0 + 1;                       // player-facing 1-based

            String lineToShow = null;
            Level lvl = Minecraft.getInstance().level;
            if (lvl != null && procId != null) {
                var opt = lvl.getRecipeManager()
                        .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                        .map(RecipeHolder::value)
                        .filter(r -> r.getDoughType().equals(procId))
                        .findFirst();

                if (opt.isPresent()) {
                    var steps = opt.get().getSteps();
                    int total = steps.size();

                    if (idx0 >= total) {
                        lineToShow = "Proofing: Finished";
                    } else {
                        // Build "1st Proof", "2nd Proof", "1st Punchdown", etc.
                        StepType type = steps.get(idx0).type();
                        // Count how many times we've seen this type up to idx0 (for the ordinal)
                        int occurrence = 0;
                        for (int i = 0; i <= idx0; i++) if (steps.get(i).type() == type) occurrence++;

                        // ordinal
                        int mod100 = occurrence % 100;
                        String ord;
                        if (mod100 >= 11 && mod100 <= 13) {
                            ord = occurrence + "th";
                        } else {
                            switch (occurrence % 10) {
                                case 1 -> ord = occurrence + "st";
                                case 2 -> ord = occurrence + "nd";
                                case 3 -> ord = occurrence + "rd";
                                default -> ord = occurrence + "th";
                            }
                        }

                        // Title-case word from enum name
                        String word = type.name().toLowerCase(Locale.ROOT);
                        word = Character.toUpperCase(word.charAt(0)) + word.substring(1);

                        String friendly = ord + " " + word;
                        lineToShow = String.format(Locale.ROOT, "Proofing: Step %d/%d — %s", idx1, total, friendly);
                    }
                }
            }

            if (lineToShow == null) {
                // Fallback if we couldn't resolve the process recipe on client
                lineToShow = String.format(Locale.ROOT, "Proofing: Step %d", idx1);
            }
            tooltip.add(Component.literal(lineToShow).withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // ── Minimal by default; full details on Shift ──
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.literal("Hold ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("Shift").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" for ingredients & baker’s %").withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }

        // — Detailed view (Shift held) —
        tooltip.add(Component.literal("Ingredients (baker’s %)").withStyle(ChatFormatting.GREEN));

        // Flour breakdown (from actual mg snapshot → flour fractions)
        Map<String, Double> flourPct = getFlourPercentages(stack);
        if (!flourPct.isEmpty()) {
            tooltip.add(Component.literal("• Flours").withStyle(ChatFormatting.GREEN));
            flourPct.forEach((name, frac) -> {
                int pct = (int) Math.round(frac * 100);
                tooltip.add(Component.literal(String.format(Locale.ROOT, "   - %d%% %s", pct, name))
                        .withStyle(ChatFormatting.GRAY));
            });
        }

        // Other ingredients (from target baker’s %)
        Map<String, Double> others = getOtherIngredientPercentages(stack);
        if (!others.isEmpty()) {
            tooltip.add(Component.literal("• Other").withStyle(ChatFormatting.GREEN));
            others.forEach((name, pctVal) -> {
                int pct = (int) Math.round(pctVal);
                tooltip.add(Component.literal(String.format(Locale.ROOT, "   - %d%% %s", pct, name))
                        .withStyle(ChatFormatting.GRAY));
            });
        }

        // Hydration + total weight
        int hydration = (int) Math.round(getHydration(stack));
        var wComp = stack.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        String totalLabel = (wComp != null) ? shortWeightLabelFromGrams(wComp.getWeight())
                : (dr.totalWeight() + "g");

        tooltip.add(Component.literal("Hydration: " + hydration + "%").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Weight: " + totalLabel).withStyle(ChatFormatting.GREEN));
    }


    // === “Punch down” interaction (unchanged) ==============================
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

    @Override
    public int getBarWidth(ItemStack stack) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return 0;
        return getBarWidth(stack, level);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        Level level = Minecraft.getInstance().level;
        return isProofStep(stack, level);
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

    @Override
    public int getBarColor(ItemStack stack) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return 0;
        float p = currentProofProgress(stack, level); // 0..1 or -1 if N/A
        if (p < 0f) return 0; // not shown anyway
        return Mth.hsvToRgb(p / 3.0f, 1.0f, 1.0f);
    }

    private boolean isProofStep(ItemStack stack, @Nullable Level level) {
        if (level == null) return false;

        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation processId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (state == null || processId == null) return false;

        var opt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(processId))
                .findFirst();
        if (opt.isEmpty()) return false;

        List<ProcessingStep> steps = opt.get().getSteps();
        int idx = state.stepIndex();
        if (idx < 0 || idx >= steps.size()) return false;

        return steps.get(idx).type() == StepType.PROOF;
    }

    private float currentProofProgress(ItemStack stack, @Nullable Level level) {
        if (level == null) return -1f;

        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation processId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (state == null || processId == null) return -1f;

        var opt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(processId))
                .findFirst();
        if (opt.isEmpty()) return -1f;

        List<ProcessingStep> steps = opt.get().getSteps();
        int idx = state.stepIndex();
        if (idx < 0 || idx >= steps.size()) return -1f;

        ProcessingStep step = steps.get(idx);
        if (step.type() != StepType.PROOF) return -1f;

        float dur = Math.max(1, step.durationTicks());
        float prog = Math.min(1f, Math.max(0f, (float) state.ticksInStep() / dur));
        return prog;
    }


    // === Data helpers for baker’s % and hydration ==========================

    private Map<String, Double> getFlourPercentages(ItemStack stack) {
        DoughRecipeComponent recipe = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (recipe == null) return Collections.emptyMap();

        int totalFlourMg = recipe.ingredients().stream()
                .filter(i -> i.category() == IngredientCategory.FLOUR)
                .mapToInt(IngredientInfo::milligrams)
                .sum();
        if (totalFlourMg <= 0) return Collections.emptyMap();

        Map<String, Integer> mgByType = new LinkedHashMap<>();
        for (IngredientInfo info : recipe.ingredients()) {
            if (info.category() != IngredientCategory.FLOUR) continue;

            FlourType ft = info.flourType();
            String key = (ft != null) ? ft.getId() : info.itemId();
            mgByType.merge(key, info.milligrams(), Integer::sum);
        }

        Map<String, Double> pctByType = new LinkedHashMap<>();
        for (var e : mgByType.entrySet()) {
            pctByType.put(titleCaseTokens(e.getKey()),
                    (e.getValue() * 1.0) / totalFlourMg); // fraction 0..1
        }
        return pctByType;
    }

    private Map<String, Double> getOtherIngredientPercentages(ItemStack stack) {
        BakerPctComponent pctComp = stack.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        if (pctComp == null) return Collections.emptyMap();

        Map<String, Double> map = new LinkedHashMap<>();
        for (var e : pctComp.percentages().entrySet()) {
            if (e.getKey() != IngredientCategory.FLOUR) {
                map.put(titleCaseTokens(e.getKey().name()), e.getValue());
            }
        }
        return map;
    }

    /** Hydration = WATER baker’s % (your current definition). */
    private double getHydration(ItemStack stack) {
        BakerPctComponent pctComp = stack.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        if (pctComp != null && pctComp.percentages().containsKey(IngredientCategory.WATER)) {
            return pctComp.percentages().get(IngredientCategory.WATER);
        }
        return 0.0;
    }

    private static String toTitleWord(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        String s = enumName.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String ordinal(int n) {
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }

}
