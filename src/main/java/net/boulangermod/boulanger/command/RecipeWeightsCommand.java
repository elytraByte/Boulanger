package net.boulangermod.boulanger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.boulangermod.boulanger.recipe.IngredientComponent;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.text.DecimalFormat;
import java.util.Arrays;

import java.util.stream.Collectors;


public class RecipeWeightsCommand {
    public RecipeWeightsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bw")
                        .requires(src -> src.hasPermission(0))
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .executes(ctx -> execute(ctx, ResourceLocationArgument.getId(ctx, "recipe"), 1.0))
                                .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.1))
                                        .executes(ctx -> execute(ctx,
                                                ResourceLocationArgument.getId(ctx, "recipe"),
                                                DoubleArgumentType.getDouble(ctx, "multiplier")
                                        ))
                                )
                        )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx,
                               ResourceLocation recipeId,
                               double multiplier) throws CommandSyntaxException {
        CommandSourceStack src   = ctx.getSource();
        Level              world = src.getLevel();

        // 1) fetch & unwrap
        var holder = world.getRecipeManager().byKey(recipeId);
        if (holder.isEmpty() || !(holder.get().value() instanceof RatioRecipe ratio)) {
            src.sendFailure(Component.literal("§cRecipe not found or not a ratio recipe: " + recipeId));
            return 0;
        }

        // 2) grab the raw servingWeight (total dough per 1× batch) and components
        double servingWeight     = ratio.getServingWeight();
        var    components        = ratio.getComponents();

        // 3) sum up baker’s % of **non-flour** ingredients
        double nonFlourPctSum = components.stream()
                .filter(c -> c.category() != IngredientCategory.FLOUR)
                .mapToDouble(IngredientComponent::targetPercent)
                .sum();

        // 4) back-solve the flour weight *for one serving*
        //
        //    total = flour + Σ(nonFlourPct/100 * flour)
        //  ⇒ flour = servingWeight / (1 + nonFlourPctSum/100)
        double baseFlourWeight  = servingWeight / (1.0 + nonFlourPctSum / 100.0);

        // 5) how many grams is “1% of the flour” for one serving?
        double gramsPerPct       = baseFlourWeight / 100.0;

        // 6) now we can compute every ingredient’s **single-batch** weight:
        //    weightSingle = gramsPerPct * bakerPct
        //
        //    and later we’ll multiply by the user’s multiplier to get the final weight.

        DecimalFormat df = new DecimalFormat("#.###");
        String recName   = recipeId.getPath();

        // 7) report header: includes total & flour for the whole batch
        double totalWeight = servingWeight * multiplier;
        double flourWeight = baseFlourWeight * multiplier;
        String header = String.format(
                "%s ×%s → total %s (flour %s)",
                recName,
                df.format(multiplier),
                formatWeight(totalWeight, df),
                formatWeight(flourWeight, df)
        );
        src.sendSuccess(() -> Component.literal(header).withStyle(ChatFormatting.GREEN), false);

        // 8) each component:
        for (IngredientComponent comp : components) {
            double bakerPct       = comp.targetPercent();
            double weightSingle  = gramsPerPct * bakerPct;
            double weightFinal   = weightSingle * multiplier; // apply the user’s “double batch” etc

            // display name
            String name = comp.allowedItems().size() == 1
                    ? prettify(comp.allowedItems().get(0).getPath())
                    : comp.category().toString().toLowerCase();

            String line = String.format(
                    " - %s: %s (%s%% baker’s)",
                    name,
                    formatWeight(weightFinal, df),
                    df.format(bakerPct)
            );
            src.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GREEN), false);
        }

        return 1;
    }


    private static String formatWeight(double grams, DecimalFormat df) {
        long rounded = Math.round(grams);
        if (Math.abs(grams - rounded) < 1e-6) {
            return rounded + "g";
        }
        if (grams >= 1000) {
            return df.format(grams / 1000.0) + "kg";
        }
        return df.format(grams) + "g";
    }


    private static String formatPercent(double pct, DecimalFormat df) {
        long rounded = Math.round(pct);
        if (Math.abs(pct - rounded) < 1e-6) {
            return rounded + "%";
        }
        return df.format(pct) + "%";
    }

    private static String prettify(String raw) {
        return Arrays.stream(raw.split("_"))
                .map(s -> s.substring(0,1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));
    }

}
