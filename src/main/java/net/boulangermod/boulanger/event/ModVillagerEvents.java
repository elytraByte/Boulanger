package net.boulangermod.boulanger.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.entity.ModVillagers;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.trade.BakerOffers;
import net.boulangermod.boulanger.trade.BreadTradeMatcher;
import net.boulangermod.boulanger.trade.FilteredMerchantOffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@EventBusSubscriber(modid = Boulanger.MODID)
public class ModVillagerEvents {

    @SubscribeEvent
    public static void addBakerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.BAKER.value()) return;

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        // All three buys are tolerance-aware based on the source RatioRecipe.
        trades.get(1).add((t, r) -> {
            if (!(t.level() instanceof ServerLevel level)) return null;
            ItemStack display = BakerOffers.baguetteBread(level).copyWithCount(1);
            return makeFilteredBuyOffer(
                    level,
                    display,
                    rl("baguette"),
                    /*emeralds*/ 6, /*maxUses*/ 12, /*xp*/ 2
            );
        });

        trades.get(1).add((t, r) -> {
            if (!(t.level() instanceof ServerLevel level)) return null;
            ItemStack display = BakerOffers.wholeWheatBread(level).copyWithCount(1);
            return makeFilteredBuyOffer(
                    level,
                    display,
                    rl("whole_wheat_bread"),
                    /*emeralds*/ 6, /*maxUses*/ 12, /*xp*/ 2
            );
        });

        trades.get(1).add((t, r) -> {
            if (!(t.level() instanceof ServerLevel level)) return null;
            ItemStack display = BakerOffers.banhMiBread(level).copyWithCount(1);
            return makeFilteredBuyOffer(
                    level,
                    display,
                    rl("banh_mi"),
                    /*emeralds*/ 6, /*maxUses*/ 12, /*xp*/ 2
            );
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, path);
    }

    private static MerchantOffer makeFilteredBuyOffer(ServerLevel level,
                                                      ItemStack displayBread,
                                                      ResourceLocation ratioId,
                                                      int emeralds, int maxUses, int villagerXp) {
        // Build tolerance from the recipe (serving_weight * tolerance)
        BreadTradeMatcher.Tolerance tol = toleranceFor(level, ratioId);

        // Sensible fallback if the recipe has no tolerance field
        if (tol.perIngredientMg() == 0 && tol.totalMg() == 0) {
            // Allow for rounding differences like 179g vs 179.548g
            tol = new BreadTradeMatcher.Tolerance(/*per-ingredient*/ 750, /*total*/ 5000);
        }

        var predicate = BreadTradeMatcher.fromDisplay(displayBread, tol);

        ItemStack preview = displayBread.copyWithCount(1);      // what shows in the UI (humanized grams)
        ItemStack result  = new ItemStack(Items.EMERALD, emeralds);

        // Accepts any stack that passes the predicate, but displays the preview
        return new net.boulangermod.boulanger.trade.FilteredMerchantOffer(
                preview, result, maxUses, villagerXp, 0.05F, predicate
        );
    }



    /**
     * Derive tolerances from the RatioRecipe itself:
     *   totalTolMg  = serving_weight * tolerance * 1000
     *   perIngTolMg = max(500 mg, totalTolMg / 3)
     *
     * This lets recipes drive strictness. (banh_mi: 180g, 5% → ~9000 mg total; whole_wheat_bread: 680g, 5% → ~34000 mg total). :contentReference[oaicite:0]{index=0} :contentReference[oaicite:1]{index=1}
     */
    private static BreadTradeMatcher.Tolerance toleranceFor(ServerLevel level, ResourceLocation ratioId) {
        var holder = level.getRecipeManager().byKey(ratioId);
        if (holder.isPresent() && holder.get().value() instanceof RatioRecipe rr) {
            double serving = servingWeight(rr);
            double tolFrac = tolerance(rr);
            int totalTolMg = (int) Math.round(serving * 1000.0 * tolFrac);
            int perIngTolMg = Math.max(500, totalTolMg / 3);
            return new BreadTradeMatcher.Tolerance(perIngTolMg, totalTolMg);
        }
        // Fallback to exact match if recipe isn't found
        return new BreadTradeMatcher.Tolerance(0, 0);
    }

    // Reflection accessors (mirrors what you used elsewhere)
    private static double servingWeight(RatioRecipe rr) {
        try {
            Number n = (Number) callAny(rr, new String[]{
                    "getServingWeight", "servingWeight", "getServingWeightGrams", "servingWeightGrams"
            });
            if (n != null) return n.doubleValue();
            for (String fName : new String[]{"servingWeight", "servingWeightGrams"}) {
                Field f = rr.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object v = f.get(rr);
                if (v instanceof Number nn) return nn.doubleValue();
            }
        } catch (Throwable ignored) {}
        return 0.0;
    }

    private static double tolerance(RatioRecipe rr) {
        try {
            Number n = (Number) callAny(rr, new String[]{"getTolerance", "tolerance", "getTolerancePercent", "tolerancePercent"});
            if (n != null) return Math.max(0.0, n.doubleValue());
            for (String fName : new String[]{"tolerance", "tolerancePercent"}) {
                Field f = rr.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object v = f.get(rr);
                if (v instanceof Number nn) return Math.max(0.0, nn.doubleValue());
            }
        } catch (Throwable ignored) {}
        return 0.0;
    }

    private static Object callAny(Object target, String[] names) {
        for (String n : names) {
            try {
                Method m = target.getClass().getMethod(n);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) { return null; }
        }
        return null;
    }
}
