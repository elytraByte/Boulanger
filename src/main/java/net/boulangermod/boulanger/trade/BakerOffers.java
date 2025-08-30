package net.boulangermod.boulanger.trade;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.dev.DoughFactory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class BakerOffers {

    // -------- Public entry points the villager listings call --------

    public static ItemStack baguetteBread(ServerLevel level) {
        return fromRatio(level, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette"), PanType.BAGUETTE);
    }

    public static ItemStack wholeWheatBread(ServerLevel level) {
        return fromRatio(level, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "whole_wheat_bread"), PanType.LOAF);
    }

    public static ItemStack banhMiBread(ServerLevel level) {
        return fromRatio(level, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "banh_mi"), PanType.BAGUETTE);
    }

    // -------- Core builder (uses live recipes + tolerance) --------

    private static ItemStack fromRatio(ServerLevel level, ResourceLocation ratioId, @Nullable PanType panOverride) {
        var rm = level.getRecipeManager();
        var holder = rm.byKey(ratioId);
        if (holder.isEmpty() || !(holder.get().value() instanceof RatioRecipe rr)) {
            // Fallback: non-matching bread so the offer won’t accept anything accidentally
            return new ItemStack(net.boulangermod.boulanger.item.ModItems.BREAD.get());
        }

        // Link process (normalized)
        ResourceLocation procRaw = findLinkedDoughProcessId(level, ratioId);
        DoughProcessRecipe proc = null;
        if (procRaw != null) {
            var ph = rm.byKey(procRaw);
            if (ph.isPresent() && ph.get().value() instanceof DoughProcessRecipe p) proc = p;
        }
        ResourceLocation procNorm = (procRaw != null) ? DoughFactory.normalizeProcessId(procRaw) : null;

        // Baker’s % by category (same as DoughFactory)
        Map<IngredientCategory, Double> pctByCat = getPctByCategory(rr);

        // Build milligram ingredients with correct FLOUR-basis math, and HUMANIZE so
        // non-additives are in whole grams (water adjusted to keep total == serving).
        var built = DoughFactory.buildIngredientInfosFromRatio(rr, pctByCat, proc, /*servings*/1, /*humanize*/true);

        // Resolve bread/pan
        BreadType breadType = BreadType.fromRecipeId(ratioId).orElse(BreadType.BAGUETTE);
        PanType pan = (panOverride != null)
                ? panOverride
                : (proc != null && proc.getPanType() != null ? PanType.byId(proc.getPanType()) : null);

        // Make the stack from the *ingredients mg* first (grams = sum(mg)/1000 rounded)
        ItemStack stack = buildBread(
                ratioId,
                breadType,
                pan,
                built.ingredients(),
                canonicalizeTargets(pctByCat)
        );

        // Stamp normalized process id
        if (procNorm != null) {
            stack.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), procNorm);
        }

        // Apply ratio tolerance to the grams we stamp, without pushing upward unnecessarily.
        int gramsCurrent = Math.round(built.totalMilligrams() / 1000f);
        int gramsAdj = toleranceAdjustedGrams(gramsCurrent, rr);
        stack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), WeightComponent.ofGrams((float) gramsAdj));

        // Rewrite DOUGH_RECIPE with the adjusted total to keep everything internally consistent
        stack.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(ratioId,
                        built.ingredients(),
                        gramsAdj,
                        canonicalizeTargets(pctByCat)));

        return stack;
    }

    // -------- Helpers copied to keep BakerOffers standalone --------

    // Build a bread stack from pieces. NOTE: does NOT set process id (caller does).
    private static ItemStack buildBread(ResourceLocation recipeId,
                                        BreadType breadType,
                                        @Nullable PanType panType,
                                        List<IngredientInfo> ingredientsMg,
                                        Map<IngredientCategory, Double> targetsPct) {
        ItemStack stack = new ItemStack(net.boulangermod.boulanger.item.ModItems.BREAD.get(), 1);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(breadType.getModelIndex()));
        stack.set(ModDataComponentTypes.BREAD_TYPE.get(), breadType);
        if (panType != null) stack.set(ModDataComponentTypes.PAN_TYPE.get(), PanTypeComponent.of(panType));

        int totalMg = ingredientsMg.stream().mapToInt(IngredientInfo::milligrams).sum();
        int totalGrams = Math.round(totalMg / 1000f);

        stack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), WeightComponent.ofGrams((float) totalGrams));
        stack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), BakerPctComponent.of(targetsPct));
        stack.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(recipeId, ingredientsMg, totalGrams, targetsPct));

        return stack;
    }

    private static LinkedHashMap<IngredientCategory, Double> canonicalizeTargets(Map<IngredientCategory, Double> in) {
        return in.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Enum::ordinal)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /** Same reflection logic as DoughFactory.getPctByCategory() */
    private static Map<IngredientCategory, Double> getPctByCategory(RatioRecipe rr) {
        try {
            Method m = rr.getClass().getMethod("percentByCategory");
            @SuppressWarnings("unchecked")
            Map<IngredientCategory, Double> typed = (Map<IngredientCategory, Double>) m.invoke(rr);
            if (typed != null && !typed.isEmpty()) return new EnumMap<>(typed);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) { e.printStackTrace(); }

        // Fallback: sum over the ratio components
        List<?> comps = getRatioComponents(rr);
        Map<IngredientCategory, Double> out = new EnumMap<>(IngredientCategory.class);
        if (comps != null) {
            for (Object c : comps) {
                IngredientCategory cat = (IngredientCategory) callAny(c, new String[]{"category", "getCategory"});
                if (cat == null) continue;
                Number pctNum = (Number) callAny(c, new String[]{
                        "percent","getPercent","percentage","getPercentage",
                        "bakersPercent","getBakersPercent","value","getValue","ratio","getRatio"
                });
                if (pctNum == null) pctNum = firstNumericField(c);
                double pct = pctNum == null ? 0.0 : pctNum.doubleValue();
                out.merge(cat, pct, Double::sum);
            }
        }
        return out;
    }

    private static @Nullable ResourceLocation findLinkedDoughProcessId(ServerLevel level, ResourceLocation ratioId) {
        var list = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get());
        for (var h : list) {
            DoughProcessRecipe proc = h.value();
            if (ratioId.equals(proc.getDoughType())) return h.id();
        }
        return null;
    }

    private static List<?> getRatioComponents(RatioRecipe rr) {
        try {
            var m = rr.getClass().getMethod("components");
            Object o = m.invoke(rr);
            if (o instanceof List<?> l) return l;
        } catch (NoSuchMethodException ignored) {
            try {
                var m = rr.getClass().getMethod("getComponents");
                Object o = m.invoke(rr);
                if (o instanceof List<?> l) return l;
            } catch (NoSuchMethodException ignored2) {
            } catch (Exception e) { e.printStackTrace(); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private static Object callAny(Object target, String[] names) {
        for (String n : names) {
            try {
                Method m = target.getClass().getMethod(n);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) { e.printStackTrace(); return null; }
        }
        return null;
    }

    private static Number firstNumericField(Object c) {
        try {
            for (Field f : c.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Class<?> t = f.getType();
                if (t == double.class || t == Double.class ||
                        t == float.class  || t == Float.class  ||
                        t == int.class    || t == Integer.class) {
                    Object v = f.get(c);
                    if (v instanceof Number n) return n;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // -------- Tolerance helpers (read from RatioRecipe) --------

    private static int toleranceAdjustedGrams(int grams, RatioRecipe rr) {
        double serving = getServingWeight(rr);
        double tol = getTolerancePercent(rr); // e.g., 0.03 for ±3%
        if (serving <= 0 || tol <= 0) return grams;

        int min = (int)Math.floor(serving * (1.0 - tol));
        int max = (int)Math.ceil (serving * (1.0 + tol));
        if (grams < min) return min;
        if (grams > max) return max;
        return grams; // already within tolerance; don’t round up unnecessarily
    }

    private static double getServingWeight(RatioRecipe rr) {
        try {
            Number n = (Number) callAny(rr, new String[]{
                    "getServingWeight","servingWeight","getServingWeightGrams","servingWeightGrams"
            });
            if (n != null) return n.doubleValue();
            for (String fName : new String[]{"servingWeight","servingWeightGrams"}) {
                Field f = rr.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object v = f.get(rr);
                if (v instanceof Number nn) return nn.doubleValue();
            }
        } catch (Throwable ignored) {}
        return 0.0;
    }

    private static double getTolerancePercent(RatioRecipe rr) {
        try {
            Number n = (Number) callAny(rr, new String[]{
                    "getTolerance","tolerance","getTolerancePercent","tolerancePercent"
            });
            if (n != null) return n.doubleValue();
            for (String fName : new String[]{"tolerance","tolerancePercent"}) {
                Field f = rr.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object v = f.get(rr);
                if (v instanceof Number nn) return nn.doubleValue();
            }
        } catch (Throwable ignored) {}
        return 0.0;
    }

    private BakerOffers() {}

}
