package net.boulangermod.boulanger.command;

import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.util.dev.DoughFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public final class DevTestCommands {
    private DevTestCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bdebug")
                        .requires(src -> src.hasPermission(2))
                        .then(
                                Commands.literal("give-dough")
                                        .then(Commands.argument("ratio_id", ResourceLocationArgument.id())
                                                .suggests(RATIO_ID_SUGGESTIONS)
                                                .then(Commands.argument("servings", integer(1))
                                                        .then(Commands.argument("proof_step", integer(1))
                                                                .executes(ctx -> {
                                                                    CommandSourceStack src = ctx.getSource();
                                                                    ResourceLocation ratioId = ResourceLocationArgument.getId(ctx, "ratio_id");
                                                                    int servings = getInteger(ctx, "servings");
                                                                    int proofStep = getInteger(ctx, "proof_step");
                                                                    return execGiveDough(src, ratioId, servings, proofStep); // may throw
                                                                })
                                                        )
                                                )
                                        )
                        )
                        .then(
                                Commands.literal("give-bread")
                                        .then(Commands.argument("ratio_id", ResourceLocationArgument.id())
                                                .suggests(RATIO_ID_SUGGESTIONS)
                                                .then(Commands.argument("servings", integer(1))
                                                        .executes(ctx -> {
                                                            CommandSourceStack src = ctx.getSource();
                                                            ResourceLocation ratioId = ResourceLocationArgument.getId(ctx, "ratio_id");
                                                            int servings = getInteger(ctx, "servings");
                                                            return execGiveBread(src, ratioId, servings); // may throw
                                                        })
                                                )
                                        )
                        )
        );
    }

    // ── Handlers ─────────────────────────────────────────────────────────

    private static int execGiveDough(CommandSourceStack src,
                                     ResourceLocation ratioId,
                                     int servings,
                                     int proofStep) throws CommandSyntaxException {  // ← add throws
        ServerLevel level = src.getLevel();
        Player player = src.getPlayerOrException();                                   // ← fine now

        ItemStack stack = DoughFactory.createDoughFromRatio(level, ratioId, servings, proofStep);
        if (stack.isEmpty()) {
            src.sendFailure(Component.literal("[Boulanger] Failed to create dough for " + ratioId));
            return 0;
        }
        giveToOrDrop(player, stack);
        src.sendSuccess(() -> Component.literal(
                "[Boulanger] Gave PROOF#" + proofStep + " dough for " + ratioId + " ×1"), false);
        return 1;
    }

    private static int execGiveBread(CommandSourceStack src,
                                     ResourceLocation ratioId,
                                     int servings) throws CommandSyntaxException {   // ← add throws
        ServerLevel level = src.getLevel();
        Player player = src.getPlayerOrException();                                   // ← fine now

        ItemStack stack = DoughFactory.createBreadFromRatio(level, ratioId, servings);
        if (stack.isEmpty()) {
            src.sendFailure(Component.literal("[Boulanger] Failed to create bread for " + ratioId));
            return 0;
        }
        giveToOrDrop(player, stack);
        src.sendSuccess(() -> Component.literal(
                "[Boulanger] Gave bread for " + ratioId + " ×1"), false);
        return 1;
    }

    // ── Suggestions ──────────────────────────────────────────────────────

    private static final SuggestionProvider<CommandSourceStack> RATIO_ID_SUGGESTIONS = (ctx, builder) -> {
        suggestRatioIds(ctx.getSource().getServer().getRecipeManager(), builder);
        return builder.buildFuture();
    };

    private static void suggestRatioIds(RecipeManager rm, SuggestionsBuilder builder) {
        List<RecipeHolder<RatioRecipe>> list = rm.getAllRecipesFor(ModRecipeSerializers.RATIO_TYPE.get());
        for (RecipeHolder<RatioRecipe> h : list) builder.suggest(h.id().toString());
    }

    // ── Utils ────────────────────────────────────────────────────────────

    private static void giveToOrDrop(Player player, ItemStack stack) {
        boolean added = player.getInventory().add(stack);
        if (!added) player.drop(stack, false);
    }
}
