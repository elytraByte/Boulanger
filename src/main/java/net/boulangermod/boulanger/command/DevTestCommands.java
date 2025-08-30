package net.boulangermod.boulanger.command;

import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.dev.DoughFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public final class DevTestCommands {
    private DevTestCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bdebug")
                        .requires(src -> src.hasPermission(2))
                        // ---- existing give-dough / give-bread blocks unchanged ----
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
                                                                    return execGiveDough(src, ratioId, servings, proofStep);
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
                                                            return execGiveBread(src, ratioId, servings);
                                                        })
                                                )
                                        )
                        )
                        // ---------------- NEW: scan-only dump-trades ----------------
                        .then(
                                Commands.literal("dump-trades")
                                        .executes(ctx -> dumpTradesNearby(ctx.getSource(), 12))                 // default radius=12
                                        .then(Commands.argument("radius", integer(1, 64))
                                                .executes(ctx -> dumpTradesNearby(ctx.getSource(), getInteger(ctx, "radius"))))
                        )
        );
    }

    // Scan for villagers within <radius> blocks of the player and dump their offers.
    private static int dumpTradesNearby(CommandSourceStack src, int radius) {
        Player p = src.getPlayer();
        if (p == null) return 0;

        Level level = p.level();
        // axis-aligned cube centered on player
        AABB box = AABB.ofSize(p.position(), radius * 2.0, radius * 2.0, radius * 2.0);
        List<AbstractVillager> villagers = level.getEntitiesOfClass(AbstractVillager.class, box);

        if (villagers.isEmpty()) {
            src.sendFailure(Component.literal("No villager within " + radius + " blocks."));
            return 0;
        }

        src.sendSuccess(() -> Component.literal(
                "Scanning " + villagers.size() + " villager(s) within " + radius + " blocks..."), false);

        int idxVillager = 0;
        for (AbstractVillager v : villagers) {
            MerchantOffers offers = v.getOffers();
            String name = v.getName().getString();
            String pos = v.blockPosition().toShortString();
            int finalIdxVillager = idxVillager;
            src.sendSuccess(() -> Component.literal(
                    String.format("Villager #%d %s @ %s → offers: %d", finalIdxVillager, name, pos, offers.size())), false);

            dumpOffers(src, offers);
            idxVillager++;
        }
        return 1;
    }

    // Unchanged: dumpOffers(...) and describe(...) are reused to print each ItemStack
    private static void dumpOffers(CommandSourceStack src, MerchantOffers offers) {
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer o = offers.get(i);
            int finalI = i;
            src.sendSuccess(() -> Component.literal("— Offer #" + finalI), false);
            describe(src, "A", o.getBaseCostA());
            ItemStack b = o.getCostB();
            if (!b.isEmpty()) describe(src, "B", b);
            describe(src, "R", o.getResult());
        }
    }

    private static void describe(CommandSourceStack src, String label, ItemStack s) {
        ResourceLocation id = s.getItemHolder().unwrapKey()
                .map(k -> k.location())
                .orElse(Items.AIR.builtInRegistryHolder().unwrapKey().get().location());

        StringBuilder sb = new StringBuilder();
        sb.append(label).append(": ").append(id).append(" x").append(s.getCount());

        CustomModelData cmd = s.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) sb.append(", cmd=").append(cmd.value());

        PanTypeComponent pan = s.get(ModDataComponentTypes.PAN_TYPE.get());
        if (pan != null) sb.append(", pan=").append(pan.toPanType().getId()); // or pan.type().getId() depending on your API

        BreadType bt = s.get(ModDataComponentTypes.BREAD_TYPE.get());
        if (bt != null) sb.append(", bread_type=").append(bt.getId());

        ResourceLocation proc = s.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (proc != null) sb.append(", process=").append(proc);

        WeightComponent w = s.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (w != null) sb.append(", grams=").append(Math.round(w.getWeight()));

        BakerPctComponent pct = s.get(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        if (pct != null) sb.append(", pctKeys=").append(pct.percentages().keySet());

        DoughRecipeComponent dr = s.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr != null) {
            sb.append(", recipeId=").append(dr.recipeId())
                    .append(", totalWeight=").append(dr.totalWeight())
                    .append(", ingredients=").append(dr.ingredients().size());
        }

        src.sendSuccess(() -> Component.literal(sb.toString()), false);

        if (dr != null) {
            for (var ii : dr.ingredients()) {
                String flour = (ii.flourType() != null) ? ii.flourType().getId() : "-";
                src.sendSuccess(() -> Component.literal(
                        String.format("   • %s [%s] %dmg%s",
                                ii.itemId(), ii.category().name(), ii.milligrams(),
                                (ii.category() == IngredientCategory.FLOUR ? " (" + flour + ")" : "")
                        )
                ), false);
            }
        }
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
