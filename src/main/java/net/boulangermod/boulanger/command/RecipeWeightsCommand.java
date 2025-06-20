// src/main/java/net/boulangermod/boulanger/command/RecipeWeightsCommand.java
package net.boulangermod.boulanger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Optional;
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

        // fetch & unwrap
        var optHolder = world.getRecipeManager().byKey(recipeId);
        if (optHolder.isEmpty()) {
            src.sendFailure(Component.literal("§cRecipe not found: " + recipeId));
            return 0;
        }
        Recipe<?> raw = optHolder.get().value();
        if (!(raw instanceof RatioRecipe ratio)) {
            src.sendFailure(Component.literal("§cNot a ratio recipe: " + recipeId));
            return 0;
        }

        // compute total
        double totalWeight = ratio.getServingWeight() * multiplier;
        String recName     = recipeId.getPath();  // no namespace

        // decimal formatter for up to 3 places
        DecimalFormat df = new DecimalFormat("#.###");

        // send header
        String header = String.format(
                "%s ×%s → total %s",
                recName,
                df.format(multiplier),
                formatWeight(totalWeight, df)
        );
        src.sendSuccess(() -> Component.literal(header)
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        // each component
        for (var comp : ratio.getComponents()) {
            double pct = comp.targetPercent();
            double w   = totalWeight * pct / 100.0;

            // get the raw path (text after the colon)
            String displayName;
            if (comp.allowedItems().size() == 1) {
                ResourceLocation rl = comp.allowedItems().get(0);
                displayName = prettify(rl.getPath());
            } else {
                displayName = comp.category().toString().toLowerCase();
            }

            String line = String.format(
                    " - %s: %s (%s)",
                    displayName,
                    formatWeight(w, df),
                    formatPercent(pct, df)
            );
            src.sendSuccess(() -> Component.literal(line)
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
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
