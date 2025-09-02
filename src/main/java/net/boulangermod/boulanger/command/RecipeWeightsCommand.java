package net.boulangermod.boulanger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.boulangermod.boulanger.recipe.IngredientComponent;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * /bw <recipe> [multiplier] [basis=auto|serving|batch] [unit=loaf|roll]
 *
 * Semantics:
 *  - Serving = one unit (loaf or roll). We choose the specified unit; else prefer loaf, then roll.
 *  - Batch   = derived total: 12 × the resolved serving (or 12 × baseline when no sizes exist).
 *  - Auto    = prefer serving (loaf/roll if available) → else derived batch → else baseline.
 */
public class RecipeWeightsCommand {

    // ---------- Config --------------------------------------------------------
    private static final int DEFAULT_UNITS_PER_BATCH = 12;
    private static final DecimalFormat DF = new DecimalFormat("#.###");

    // ---------- Suggesters ----------------------------------------------------

    // Recipes (only RatioRecipe). Manual suggestion build to support tooltips in all mappings.
    private static final SuggestionProvider<CommandSourceStack> RATIO_RECIPE_SUGGESTER = (ctx, builder) -> {
        Level level = ctx.getSource().getLevel();
        var mgr = level.getRecipeManager();
        var holders = mgr.getAllRecipesFor(ModRecipeSerializers.RATIO_TYPE.get()); // List<RecipeHolder<RatioRecipe>>

        for (RecipeHolder<RatioRecipe> rh : holders) {
            ResourceLocation id = rh.id();
            RatioRecipe r = rh.value();

            Integer loaf = r.getLoafSizeG(); // <- rename if your getters differ
            Integer roll = r.getRollSizeG(); // <-

            double derivedBatch = deriveBatchTotalG(r, /*unit*/ null, nonFlourPctSum(r));

            StringBuilder tip = new StringBuilder();
            if (loaf != null) tip.append("loaf ≈ ").append(formatWeight(loaf, DF));
            if (roll != null) {
                if (tip.length() > 0) tip.append("; ");
                tip.append("roll ≈ ").append(formatWeight(roll, DF));
            }
            if (tip.length() > 0) tip.append("; ");
            tip.append("batch (×").append(DEFAULT_UNITS_PER_BATCH).append(") ≈ ").append(formatWeight(derivedBatch, DF));

            builder.suggest(id.toString(), Component.literal(tip.toString()));
        }
        return builder.buildFuture();
    };

    // Basis suggestion: auto, serving, batch — with tooltips (manual build).
    private static final SuggestionProvider<CommandSourceStack> BASIS_SUGGESTER = (ctx, builder) -> {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "recipe");
        Level level = ctx.getSource().getLevel();
        var holder = level.getRecipeManager().byKey(id);

        // Always offer these three
        var opts = List.of("auto", "serving", "batch");

        if (holder.isPresent() && holder.get().value() instanceof RatioRecipe r) {
            double nonFlourPct = nonFlourPctSum(r);
            Double serving = resolveServingWeightG(r, /*unit*/ null);
            double batch = deriveBatchTotalG(r, /*unit*/ null, nonFlourPct);

            builder.suggest("auto", Component.literal("Prefer serving → batch → baseline"));
            builder.suggest("serving", Component.literal(serving != null
                    ? "One unit ≈ " + formatWeight(serving, DF)
                    : "No sizes; baseline 100g flour"));
            builder.suggest("batch", Component.literal("×" + DEFAULT_UNITS_PER_BATCH + " of serving ≈ " + formatWeight(batch, DF)));
        } else {
            builder.suggest("auto", Component.literal("Prefer serving → batch → baseline"));
            builder.suggest("serving", Component.literal("One unit (loaf/roll)"));
            builder.suggest("batch", Component.literal("×" + DEFAULT_UNITS_PER_BATCH + " of serving"));
        }
        return builder.buildFuture();
    };

    // Unit suggestion: loaf/roll depending on available sizes (manual build).
    private static final SuggestionProvider<CommandSourceStack> UNIT_SUGGESTER = (ctx, builder) -> {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "recipe");
        Level level = ctx.getSource().getLevel();
        var holder = level.getRecipeManager().byKey(id);

        if (holder.isPresent() && holder.get().value() instanceof RatioRecipe r) {
            if (r.getLoafSizeG() != null) {
                builder.suggest("loaf", Component.literal("≈ " + formatWeight(r.getLoafSizeG(), DF)));
            }
            if (r.getRollSizeG() != null) {
                builder.suggest("roll", Component.literal("≈ " + formatWeight(r.getRollSizeG(), DF)));
            }
        }
        return builder.buildFuture();
    };

    // Multiplier suggestion: show final totals based on current basis + unit selection (manual build).
    private static final SuggestionProvider<CommandSourceStack> MULTIPLIER_SUGGESTER = (ctx, builder) -> {
        var candidates = List.of("0.5", "1", "2", "3", "5", "10");
        try {
            ResourceLocation id = ResourceLocationArgument.getId(ctx, "recipe");
            String basis = getOptional(ctx, "basis");
            String unit  = getOptional(ctx, "unit");

            Level level = ctx.getSource().getLevel();
            var holder = level.getRecipeManager().byKey(id);
            if (holder.isPresent() && holder.get().value() instanceof RatioRecipe r) {
                UnitWeight uw = resolveUnitWeight(r, basis, unit);
                for (String s : candidates) {
                    try {
                        double m = Double.parseDouble(s);
                        builder.suggest(s, Component.literal("→ total " + formatWeight(uw.unitTotalG * m, DF)));
                    } catch (NumberFormatException ignored) {}
                }
                return builder.buildFuture();
            }
        } catch (Exception ignored) {}

        for (String s : candidates) builder.suggest(s);
        return builder.buildFuture();
    };

    // ---------- Registration --------------------------------------------------

    public RecipeWeightsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bw")
                        .requires(src -> src.hasPermission(0))
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .suggests(RATIO_RECIPE_SUGGESTER)

                                // /bw <recipe>
                                .executes(ctx -> execute(ctx, ResourceLocationArgument.getId(ctx, "recipe"), 1.0, "auto", null))

                                // /bw <recipe> <multiplier>
                                .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.1))
                                        .suggests(MULTIPLIER_SUGGESTER)
                                        .executes(ctx -> execute(
                                                ctx,
                                                ResourceLocationArgument.getId(ctx, "recipe"),
                                                DoubleArgumentType.getDouble(ctx, "multiplier"),
                                                "auto",
                                                null
                                        ))
                                        // /bw <recipe> <multiplier> <basis>
                                        .then(Commands.argument("basis", StringArgumentType.word())
                                                .suggests(BASIS_SUGGESTER)
                                                .executes(ctx -> execute(
                                                        ctx,
                                                        ResourceLocationArgument.getId(ctx, "recipe"),
                                                        DoubleArgumentType.getDouble(ctx, "multiplier"),
                                                        StringArgumentType.getString(ctx, "basis"),
                                                        null
                                                ))
                                                // /bw <recipe> <multiplier> <basis> <unit>
                                                .then(Commands.argument("unit", StringArgumentType.word())
                                                        .suggests(UNIT_SUGGESTER)
                                                        .executes(ctx -> execute(
                                                                ctx,
                                                                ResourceLocationArgument.getId(ctx, "recipe"),
                                                                DoubleArgumentType.getDouble(ctx, "multiplier"),
                                                                StringArgumentType.getString(ctx, "basis"),
                                                                StringArgumentType.getString(ctx, "unit")
                                                        ))
                                                )
                                        )
                                        // /bw <recipe> <multiplier> <unit>
                                        .then(Commands.argument("unit", StringArgumentType.word())
                                                .suggests(UNIT_SUGGESTER)
                                                .executes(ctx -> execute(
                                                        ctx,
                                                        ResourceLocationArgument.getId(ctx, "recipe"),
                                                        DoubleArgumentType.getDouble(ctx, "multiplier"),
                                                        "auto",
                                                        StringArgumentType.getString(ctx, "unit")
                                                ))
                                        )
                                )
                                // /bw <recipe> <basis>
                                .then(Commands.argument("basis", StringArgumentType.word())
                                        .suggests(BASIS_SUGGESTER)
                                        .executes(ctx -> execute(
                                                ctx,
                                                ResourceLocationArgument.getId(ctx, "recipe"),
                                                1.0,
                                                StringArgumentType.getString(ctx, "basis"),
                                                null
                                        ))
                                        // /bw <recipe> <basis> <unit>
                                        .then(Commands.argument("unit", StringArgumentType.word())
                                                .suggests(UNIT_SUGGESTER)
                                                .executes(ctx -> execute(
                                                        ctx,
                                                        ResourceLocationArgument.getId(ctx, "recipe"),
                                                        1.0,
                                                        StringArgumentType.getString(ctx, "basis"),
                                                        StringArgumentType.getString(ctx, "unit")
                                                ))
                                        )
                                )
                                // /bw <recipe> <unit>
                                .then(Commands.argument("unit", StringArgumentType.word())
                                        .suggests(UNIT_SUGGESTER)
                                        .executes(ctx -> execute(
                                                ctx,
                                                ResourceLocationArgument.getId(ctx, "recipe"),
                                                1.0,
                                                "auto",
                                                StringArgumentType.getString(ctx, "unit")
                                        ))
                                )
                        )
        );
    }

    // ---------- Execution -----------------------------------------------------

    private static int execute(CommandContext<CommandSourceStack> ctx,
                               ResourceLocation recipeId,
                               double multiplier,
                               String basis,
                               @Nullable String unit) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        Level world = src.getLevel();

        var holder = world.getRecipeManager().byKey(recipeId);
        if (holder.isEmpty() || !(holder.get().value() instanceof RatioRecipe ratio)) {
            src.sendFailure(Component.literal("§cRecipe not found or not a ratio recipe: " + recipeId));
            return 0;
        }

        var components = ratio.getComponents();

        // Resolve unit total + base flour for one "unit" (serving/batch/baseline)
        UnitWeight uw = resolveUnitWeight(ratio, basis, unit);

        // grams per 1% flour for one "unit"
        double gramsPerPct = uw.baseFlourG / 100.0;

        // Header
        double totalWeight = uw.unitTotalG * multiplier;
        double flourWeight = uw.baseFlourG * multiplier;

        String header = String.format(
                "%s ×%s → total %s (flour %s) [%s]",
                recipeId.getPath(),
                DF.format(multiplier),
                formatWeight(totalWeight, DF),
                formatWeight(flourWeight, DF),
                uw.label
        );
        src.sendSuccess(() -> Component.literal(header).withStyle(ChatFormatting.GREEN), false);

        // Lines
        for (IngredientComponent comp : components) {
            double bakerPct = comp.targetPercent();
            double weightSingle = gramsPerPct * bakerPct;
            double weightFinal = weightSingle * multiplier;

            String name = comp.allowedItems().size() == 1
                    ? prettify(comp.allowedItems().get(0).getPath())
                    : comp.category().toString().toLowerCase();

            String line = String.format(
                    " - %s: %s (%s%% baker’s)",
                    name,
                    formatWeight(weightFinal, DF),
                    DF.format(bakerPct)
            );
            src.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GREEN), false);
        }

        return 1;
    }

    // ---------- Helpers -------------------------------------------------------

    private record UnitWeight(double unitTotalG, double baseFlourG, String label) { }

    private static UnitWeight resolveUnitWeight(RatioRecipe ratio, @Nullable String basis, @Nullable String unit) {
        double nonFlour = nonFlourPctSum(ratio);

        String b = basis == null ? "auto" : basis.toLowerCase(Locale.ROOT);
        String u = unit == null ? null : unit.toLowerCase(Locale.ROOT);

        Double serving = resolveServingWeightG(ratio, u);

        if ("serving".equals(b)) {
            if (serving != null) {
                double baseFlour = serving / (1.0 + nonFlour / 100.0);
                String label = "per serving" + (u != null ? " [" + u + "]" : servingWas(loafOrRoll(ratio, serving)));
                return new UnitWeight(serving, baseFlour, label);
            } else {
                double baseFlour = 100.0;
                double unitTotal = baseFlour * (1.0 + nonFlour / 100.0);
                return new UnitWeight(unitTotal, baseFlour, "serving [baseline 100g flour]");
            }
        }

        if ("batch".equals(b)) {
            double batch = deriveBatchTotalG(ratio, u, nonFlour);
            double baseFlour = batch / (1.0 + nonFlour / 100.0);
            String which = serving != null ? (u != null ? u : loafOrRoll(ratio, serving)) : "baseline";
            return new UnitWeight(batch, baseFlour, "per batch [" + which + "×" + DEFAULT_UNITS_PER_BATCH + "]");
        }

        // AUTO
        if (serving != null) {
            double baseFlour = serving / (1.0 + nonFlour / 100.0);
            String label = "per serving" + (u != null ? " [" + u + "]" : servingWas(loafOrRoll(ratio, serving)));
            return new UnitWeight(serving, baseFlour, label);
        } else {
            double batch = deriveBatchTotalG(ratio, u, nonFlour);
            double baseFlour = batch / (1.0 + nonFlour / 100.0);
            String which = serving != null ? (u != null ? u : loafOrRoll(ratio, serving)) : "baseline";
            return new UnitWeight(batch, baseFlour, "per batch [" + which + "×" + DEFAULT_UNITS_PER_BATCH + "]");
        }
    }

    @Nullable
    private static Double resolveServingWeightG(RatioRecipe r, @Nullable String unit) {
        Integer loaf = r.getLoafSizeG(); // <- rename if your getters differ
        Integer roll = r.getRollSizeG(); // <-
        if ("roll".equalsIgnoreCase(unit) && roll != null) return roll.doubleValue();
        if ("loaf".equalsIgnoreCase(unit) && loaf != null) return loaf.doubleValue();
        if (loaf != null) return loaf.doubleValue();
        if (roll != null) return roll.doubleValue();
        return null;
    }

    private static String loafOrRoll(RatioRecipe r, double chosen) {
        Integer loaf = r.getLoafSizeG();
        Integer roll = r.getRollSizeG();
        if (loaf != null && Math.abs(loaf - chosen) < 1e-6) return "loaf";
        if (roll != null && Math.abs(roll - chosen) < 1e-6) return "roll";
        return "baseline";
    }

    private static String servingWas(String which) {
        return " [" + which + "]";
    }

    private static double deriveBatchTotalG(RatioRecipe r, @Nullable String unit, double nonFlourPctSum) {
        Double serving = resolveServingWeightG(r, unit);
        if (serving != null) return serving * DEFAULT_UNITS_PER_BATCH;
        // baseline "serving" built from 100g flour → then × DEFAULT_UNITS_PER_BATCH
        double baseFlour = 100.0;
        double unitTotal = baseFlour * (1.0 + nonFlourPctSum / 100.0);
        return unitTotal * DEFAULT_UNITS_PER_BATCH;
    }

    private static double nonFlourPctSum(RatioRecipe ratio) {
        return ratio.getComponents().stream()
                .filter(c -> c.category() != IngredientCategory.FLOUR)
                .mapToDouble(IngredientComponent::targetPercent)
                .sum();
    }

    private static String formatWeight(double grams, DecimalFormat df) {
        if (grams >= 1000.0) return df.format(grams / 1000.0) + "kg";
        if (grams >= 1.0) {
            long rounded = Math.round(grams);
            if (Math.abs(grams - rounded) < 1e-6) return rounded + "g";
            return df.format(grams) + "g";
        }
        if (grams >= 0.001) return Math.round(grams * 1000.0) + "mg";
        return "<1mg";
    }

    private static String prettify(String raw) {
        return Arrays.stream(raw.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    @Nullable
    private static String getOptional(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (Exception ignored) {
            return null;
        }
    }
}
