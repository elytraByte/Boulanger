package net.boulangermod.boulanger.util.dev;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Dev helper to synthesize dough/bread that looks like it came through the pipeline.
 * IMPORTANT: stepOrdinal is the ABSOLUTE process step (1..N), not the Nth PROOF.
 */
public final class DoughFactory {
    private static final Logger LOG = LogManager.getLogger("Boulanger/DoughFactory");

    /**
     * If true, round all non-additives to whole grams (nearest 1000 mg) and then
     * adjust one ingredient (preferring WATER) so the sum lands exactly on the
     * target serving grams. Additives always remain exact mg.
     *
     * Leave false for fully precise test stacks. Turn on for "human" ingredient lists.
     */
    private static final boolean HUMANIZE_NON_ADDITIVES_TO_GRAMS = false;

    private DoughFactory() {}

    // ─────────────────────────────────────────────────────────────────────
    // Public API — ABSOLUTE STEP ORDINAL (1..N)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Create a dough item from a RatioRecipe and set its state to the ABSOLUTE process step.
     *
     * @param level       server level
     * @param ratioId     id of the RatioRecipe
     * @param servings    number of servings (>=1)
     * @param stepOrdinal ABSOLUTE step index in the process (1..N). Clamped.
     * @return ItemStack DOUGH or EMPTY on failure
     */
    public static ItemStack createDoughFromRatio(ServerLevel level, ResourceLocation ratioId, int servings, int stepOrdinal) {
        RecipeManager rm = level.getRecipeManager();
        Optional<RecipeHolder<?>> holderOpt = rm.byKey(ratioId);
        if (holderOpt.isEmpty() || !(holderOpt.get().value() instanceof RatioRecipe rr)) {
            LOG.warn("No RatioRecipe found for {} (present? {} type? {})",
                    ratioId, holderOpt.isPresent(), holderOpt.map(h -> h.value().getClass().getName()).orElse("<none>"));
            return ItemStack.EMPTY;
        }

        ItemStack dough = new ItemStack(ModItems.DOUGH.get());

        // 0) Link process id (normalized onto the stack); fetch process recipe, pan, servings
        ResourceLocation procIdRaw = findLinkedDoughProcessId(level, ratioId);
        DoughProcessRecipe proc = null;
        if (procIdRaw != null) {
            dough.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), normalizeProcessId(procIdRaw));
            var holder = rm.byKey(procIdRaw);
            if (holder.isPresent() && holder.get().value() instanceof DoughProcessRecipe p) {
                proc = p;
            }
        } else {
            LOG.warn("No DoughProcessRecipe linked to ratio {}. (dough_type mismatch?)", ratioId);
        }

        // 1) Baker’s % map
        Map<IngredientCategory, Double> pctByCat = getPctByCategory(rr);
        trySetBakerPct(dough, pctByCat);

        // 2) Concrete ingredient list (+ optional human rounding) and exact total
        double tolerance = getTolerance(rr); // 0.05 in your JSON → 5%
        BuildResult built = buildIngredientInfos(rr, pctByCat, proc, Math.max(1, servings),
                HUMANIZE_NON_ADDITIVES_TO_GRAMS, /*respectTolerance*/ true, tolerance);

        // 3) Total weight component (from serving size, not from mg accumulation)
        dough.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), WeightComponent.ofGrams((float) built.totalGrams()));

        // 4) DoughRecipe snapshot (ratio id + target map + ingredients)
        dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(ratioId, pctByCat, built.ingredients(), built.totalGrams()));

        // 5) Proofing state by ABSOLUTE step (never shaped on spawn)
        if (proc != null) {
            dough.set(ModDataComponentTypes.PROOFING_STATE.get(), computeStateByAbsoluteStep(proc, stepOrdinal));
            // Ensure no PAN on dough at spawn (shaping happens at table)
            try {
                if (dough.has(ModDataComponentTypes.PAN_TYPE.get())) dough.remove(ModDataComponentTypes.PAN_TYPE.get());
            } catch (Throwable ignored) {}
        } else {
            dough.set(ModDataComponentTypes.PROOFING_STATE.get(), ProofingStateComponent.finalProofed());
        }

        debugAssertDough(dough, stepOrdinal, procIdRaw);
        return dough;
    }

    /**
     * Create a bread item from a RatioRecipe.
     * Writes NORMALIZED process id; sets PAN_TYPE and BREAD_TYPE; also carries baker % & grams like dough for tooltips.
     * Additionally sets CustomModelData from the resolved BreadType so the correct texture shows up immediately.
     */
    public static ItemStack createBreadFromRatio(ServerLevel level, ResourceLocation ratioId, int servings) {
        var holderOpt = level.getRecipeManager().byKey(ratioId);
        if (holderOpt.isEmpty() || !(holderOpt.get().value() instanceof RatioRecipe rr)) {
            LOG.warn("No RatioRecipe found for {}", ratioId);
            return ItemStack.EMPTY;
        }

        ItemStack bread = new ItemStack(ModItems.BREAD.get());

        // Link process
        ResourceLocation procIdRaw = findLinkedDoughProcessId(level, ratioId);
        DoughProcessRecipe proc = null;
        if (procIdRaw != null) {
            bread.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), normalizeProcessId(procIdRaw));
            var holder = level.getRecipeManager().byKey(procIdRaw);
            if (holder.isPresent() && holder.get().value() instanceof DoughProcessRecipe p) {
                proc = p;
            }
        } else {
            LOG.warn("No DoughProcessRecipe linked to ratio {}. Bread will miss PAN/BREAD_TYPE.", ratioId);
        }

        // Baker % and concrete ingredients
        Map<IngredientCategory, Double> pctByCat = getPctByCategory(rr);
        trySetBakerPct(bread, pctByCat);

        double tolerance = getTolerance(rr);
        BuildResult built = buildIngredientInfos(rr, pctByCat, proc, Math.max(1, servings),
                HUMANIZE_NON_ADDITIVES_TO_GRAMS, /*respectTolerance*/ true, tolerance);

        bread.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), WeightComponent.ofGrams((float) built.totalGrams()));
        bread.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(ratioId, pctByCat, built.ingredients(), built.totalGrams()));

        // Resolve bread type & model
        BreadType bt = BreadType.fromRecipeId(ratioId).orElse(null);
        if (bt == null && proc != null && proc.getPanType() != null) {
            String path = proc.getPanType().getPath();
            if (path.contains("baguette")) bt = BreadType.BAGUETTE;
        }

        if (proc != null && proc.getPanType() != null) {
            bread.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(proc.getPanType().toString()));
        }
        if (bt != null) {
            bread.set(ModDataComponentTypes.BREAD_TYPE.get(), bt);
            bread.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                    new net.minecraft.world.item.component.CustomModelData(bt.getModelIndex()));
        }

        debugAssertBread(bread, procIdRaw);
        return bread;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ingredient construction
    // ─────────────────────────────────────────────────────────────────────

    public static record BuildResult(
            List<IngredientInfo> ingredients,
            int totalMilligrams,
            int totalGrams
    ) {}

    /**
     * Build the concrete ingredient list from ratio percents & serving size.
     * Optionally rounds non-additives to whole grams and adjusts one ingredient to hit the exact serving grams.
     */
    private static BuildResult buildIngredientInfos(RatioRecipe rr,
                                                    Map<IngredientCategory, Double> pctByCat,
                                                    @Nullable DoughProcessRecipe proc,
                                                    int servings,
                                                    boolean roundNonAdditivesToGrams,
                                                    boolean respectTolerance,
                                                    double toleranceFraction /* 0..1, e.g. 0.05 */) {
        servings = Math.max(1, servings);

        // Serving basis (prefer process -> ratio -> default 454g)
        double perServing = safeServingWeight(proc, rr);
        double targetGramsExact = perServing * servings;
        int targetGramsRounded = (int) Math.round(targetGramsExact);
        int targetMg = targetGramsRounded * 1000;

        // Flour basis math
        double nonFlourPct = pctByCat.entrySet().stream()
                .filter(e -> e.getKey() != IngredientCategory.FLOUR)
                .mapToDouble(Map.Entry::getValue).sum();

        double flourGrams = targetGramsExact / (1.0 + nonFlourPct / 100.0);

        LOG.info("[DoughFactory] basis → perServing={}g, servings={}, nonFlourPct={}, flourBasis={}g",
                String.format(java.util.Locale.ROOT, "%.3f", perServing),
                servings,
                String.format(java.util.Locale.ROOT, "%.3f", nonFlourPct),
                String.format(java.util.Locale.ROOT, "%.3f", flourGrams));

        List<?> comps = getRatioComponents(rr);
        List<IngredientInfo> out = new ArrayList<>();
        int sumMg = 0;

        if (comps != null) {
            for (Object raw : comps) {
                DetectedRatioComponent det = detRatioComponent(raw);
                if (det == null) continue;

                // grams → mg (precise, nearest mg)
                double gramsExact = flourGrams * (det.percent() / 100.0);
                int mg = (int) Math.round(gramsExact * 1000.0);

                ResourceLocation itemId = det.allowedIds().isEmpty()
                        ? defaultItemForCategory(det.category())
                        : det.allowedIds().get(0);

                LOG.info("[DoughFactory]   comp cat={} pct={} → {} mg ({} g) item={}",
                        det.category(),
                        String.format(java.util.Locale.ROOT, "%.3f", det.percent()),
                        mg,
                        String.format(java.util.Locale.ROOT, "%.3f", mg / 1000.0),
                        itemId);

                IngredientInfo info = IngredientInfo.ofMg(itemId.toString(), det.category(), mg);
                out.add(info);
                sumMg += mg;
            }
        }

        // Optional: round non-additives to whole grams for “human” numbers
        if (roundNonAdditivesToGrams) {
            out = roundNonAdditivesToGrams(out);
        }

        // Adjust to exact serving target (prefer WATER), but allow no-op if already exact
        int afterRoundSum = out.stream().mapToInt(IngredientInfo::milligrams).sum();
        int delta = targetMg - afterRoundSum;

        if (delta != 0) {
            // If you ever want to leverage tolerance to *skip* the nudge, do it here:
            // double allowed = respectTolerance ? Math.abs(targetMg) * toleranceFraction : 0.0;
            // if (Math.abs(delta) <= allowed) { /* leave as-is */ } else { adjust... }
            out = adjustToServing(out, targetGramsRounded);
        }

        int finalSumMg = out.stream().mapToInt(IngredientInfo::milligrams).sum();

        LOG.info("[DoughFactory] total = {} mg ({} g target, {} mg delta after adjust)",
                finalSumMg, targetGramsRounded, (targetMg - finalSumMg));

        return new BuildResult(out, finalSumMg, targetGramsRounded);
    }

    /** Prefer process serving weight; fall back to Ratio; default 454. */
    private static double safeServingWeight(@Nullable DoughProcessRecipe proc, RatioRecipe rr) {
        if (proc != null) {
            double sw = proc.getServingWeightGrams();
            if (sw > 0) return sw;
        }
        double sw = getServingWeight(rr);
        if (sw > 0) return sw;
        return 454.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Proofing state — ABSOLUTE step (1..N)
    // ─────────────────────────────────────────────────────────────────────

    private static ProofingStateComponent computeStateByAbsoluteStep(DoughProcessRecipe proc, int stepOrdinal) {
        List<?> steps = proc.getSteps();
        if (steps == null || steps.isEmpty()) {
            return ProofingStateComponent.finalProofed();
        }
        int idx = Math.max(0, Math.min(stepOrdinal - 1, steps.size() - 1)); // 1..N -> 0..N-1
        ProofingStateComponent st = new ProofingStateComponent(idx, /*ticks_in_step=*/0, /*shaped=*/false);
        return forceShaped(st, /*value=*/false);
    }

    private static ProofingStateComponent forceShaped(ProofingStateComponent st, boolean value) {
        try {
            var m = st.getClass().getMethod("withShaped", boolean.class);
            Object out = m.invoke(st, value);
            return (ProofingStateComponent) out;
        } catch (Throwable ignored) {}

        try {
            int idx   = readIntProp(st, new String[]{"stepIndex","getStepIndex"}, new String[]{"step_index"});
            int ticks = readIntProp(st, new String[]{"ticksInStep","getTicksInStep"}, new String[]{"ticks_in_step"});
            try {
                var of = st.getClass().getMethod("of", int.class, int.class, boolean.class);
                Object out = of.invoke(null, idx, ticks, value);
                return (ProofingStateComponent) out;
            } catch (NoSuchMethodException ignored) {
                var c = st.getClass().getDeclaredConstructor(int.class, int.class, boolean.class);
                c.setAccessible(true);
                return (ProofingStateComponent) c.newInstance(idx, ticks, value);
            }
        } catch (Throwable ignored) {}

        try {
            var f = st.getClass().getDeclaredField("shaped");
            f.setAccessible(true);
            f.setBoolean(st, value);
            return st;
        } catch (Throwable ignored) {}

        LOG.warn("[DoughFactory] Could not force shaped={} on ProofingStateComponent (class {}).",
                value, st.getClass().getName());
        return st;
    }

    private static int readIntProp(Object obj, String[] getters, String[] fields) {
        Object v = callAny(obj, getters);
        if (v instanceof Number n) return n.intValue();
        for (String name : fields) {
            try {
                var f = obj.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object fv = f.get(obj);
                if (fv instanceof Number n2) return n2.intValue();
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ratio component detection & helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Normalized view of any RatioRecipe component. */
    private record DetectedRatioComponent(
            IngredientCategory category,
            double percent,
            List<ResourceLocation> allowedIds
    ) {}

    @Nullable
    private static DetectedRatioComponent detRatioComponent(Object comp) {
        if (comp == null) return null;

        IngredientCategory cat = (IngredientCategory) callAny(comp, new String[]{"category", "getCategory"});
        if (cat == null) return null;

        Number pctNum = (Number) callAny(comp, new String[]{
                "percent", "getPercent", "percentage", "getPercentage",
                "bakersPercent", "getBakersPercent", "value", "getValue", "ratio", "getRatio"
        });
        if (pctNum == null) pctNum = firstNumericField(comp);
        double pct = pctNum == null ? 0.0 : pctNum.doubleValue();

        Object allowedAny = callAny(comp, new String[]{
                "allowedItemIds", "allowedItems", "allowed",
                "ingredientIds", "ids", "choices", "options", "items"
        });
        List<ResourceLocation> allowed = new ArrayList<>();
        ResourceLocation m = firstRLFromAny(allowedAny);
        if (m != null) allowed.add(m);
        if (allowed.isEmpty()) {
            for (var f : comp.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    ResourceLocation rl = firstRLFromAny(f.get(comp));
                    if (rl != null) { allowed.add(rl); break; }
                } catch (Throwable ignored) {}
            }
        }
        return new DetectedRatioComponent(cat, pct, Collections.unmodifiableList(allowed));
    }

    @Nullable
    public static List<?> getRatioComponents(RatioRecipe rr) {
        try {
            var m = rr.getClass().getMethod("components"); // record accessor
            Object o = m.invoke(rr);
            if (o instanceof List<?> l) return l;
        } catch (NoSuchMethodException ignored) {
            try {
                var m = rr.getClass().getMethod("getComponents"); // bean accessor
                Object o = m.invoke(rr);
                if (o instanceof List<?> l) return l;
            } catch (NoSuchMethodException ignored2) {
            } catch (Exception e) { e.printStackTrace(); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    @Nullable
    private static ResourceLocation firstAllowedId(Object comp) {
        DetectedRatioComponent det = detRatioComponent(comp);
        if (det == null || det.allowedIds().isEmpty()) return null;
        return det.allowedIds().get(0);
    }

    private static ResourceLocation defaultItemForCategory(IngredientCategory cat) {
        switch (cat) {
            case WATER:  return ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket");
            case YEAST:  return ResourceLocation.fromNamespaceAndPath("boulanger", "fleischmann");
            case SALT:   return ResourceLocation.fromNamespaceAndPath("boulanger", "salt_kosher");
            case FLOUR:  return ResourceLocation.fromNamespaceAndPath("boulanger", "bread_flour");
            case DAIRY:  return ResourceLocation.fromNamespaceAndPath("boulanger", "whole_milk");
            case SUGAR:  return ResourceLocation.fromNamespaceAndPath("boulanger", "brown_sugar");
            case EGGS:   return ResourceLocation.fromNamespaceAndPath("boulanger", "fancy_egg");
            case FAT:    return ResourceLocation.fromNamespaceAndPath("boulanger", "butter");
            default:     return ResourceLocation.fromNamespaceAndPath("minecraft", "air");
        }
    }

    @Nullable
    private static ResourceLocation firstRLFromAny(Object o) {
        if (o instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof ResourceLocation rl) return rl;
            if (first instanceof String s) {
                if (s.indexOf(':') < 0) {
                    return ResourceLocation.fromNamespaceAndPath("boulanger", s);
                }
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl != null) return rl;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Linking / lookups
    // ─────────────────────────────────────────────────────────────────────

    @Nullable
    private static ResourceLocation findLinkedDoughProcessId(ServerLevel level, ResourceLocation ratioId) {
        List<RecipeHolder<DoughProcessRecipe>> list =
                level.getRecipeManager().getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get());
        for (RecipeHolder<DoughProcessRecipe> h : list) {
            DoughProcessRecipe proc = h.value();
            if (ratioId.equals(proc.getDoughType())) return h.id();
        }
        return null;
    }

    @Nullable
    private static BreadType breadTypeForPan(ResourceLocation panId) {
        if (panId == null) return null;
        String p = panId.getPath();
        if (p.contains("baguette")) return BreadType.BAGUETTE;
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Baker’s math + extractors
    // ─────────────────────────────────────────────────────────────────────

    private static Map<IngredientCategory, Double> getPctByCategory(RatioRecipe rr) {
        try {
            Method m = rr.getClass().getMethod("percentByCategory");
            @SuppressWarnings("unchecked")
            Map<IngredientCategory, Double> typed = (Map<IngredientCategory, Double>) m.invoke(rr);
            if (typed != null && !typed.isEmpty()) return new EnumMap<>(typed);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) { e.printStackTrace(); }

        List<?> comps = getRatioComponents(rr);
        if (comps != null && !comps.isEmpty()) {
            Map<IngredientCategory, Double> out = new EnumMap<>(IngredientCategory.class);
            for (Object c : comps) {
                DetectedRatioComponent det = detRatioComponent(c);
                if (det == null) continue;
                out.merge(det.category(), det.percent(), Double::sum);
            }
            return out;
        }
        return new EnumMap<>(IngredientCategory.class);
    }

    private static double getServingWeight(RatioRecipe rr) {
        Number n = (Number) callAny(rr, new String[]{
                "getServingWeight", "servingWeight", "getServingWeightGrams", "servingWeightGrams"
        });
        if (n != null) return n.doubleValue();
        try {
            for (String fName : new String[]{"servingWeight", "servingWeightGrams"}) {
                Field f = rr.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object v = f.get(rr);
                if (v instanceof Number nn) return nn.doubleValue();
            }
        } catch (Throwable ignored) {}
        return 454.0;
    }

    private static double getTolerance(RatioRecipe rr) {
        Number n = (Number) callAny(rr, new String[]{"getTolerance", "tolerance"});
        if (n != null) return Math.max(0.0, n.doubleValue());
        try {
            Field f = rr.getClass().getDeclaredField("tolerance");
            f.setAccessible(true);
            Object v = f.get(rr);
            if (v instanceof Number nn) return Math.max(0.0, nn.doubleValue());
        } catch (Throwable ignored) {}
        return 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Reflection helpers & debug
    // ─────────────────────────────────────────────────────────────────────

    @Nullable
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

    @Nullable
    private static Object callAny(Object target, String[] methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) { e.printStackTrace(); return null; }
        }
        return null;
    }

    private static void trySetBakerPct(ItemStack stack, Map<IngredientCategory, Double> pct) {
        try {
            Constructor<BakerPctComponent> c1 = BakerPctComponent.class.getDeclaredConstructor(Map.class);
            c1.setAccessible(true);
            BakerPctComponent inst = c1.newInstance(pct);
            stack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), inst);
            return;
        } catch (Throwable ignored) {}
        try {
            Method of = BakerPctComponent.class.getMethod("of", Map.class);
            BakerPctComponent inst = (BakerPctComponent) of.invoke(null, pct);
            stack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), inst);
            return;
        } catch (Throwable ignored) {}
        LOG.warn("Could not set BAKER_PERCENTAGES (constructor/static of(Map) not found).");
    }

    private static void debugAssertDough(ItemStack dough, int stepOrdinal, @Nullable ResourceLocation procIdRaw) {
        boolean hasProc   = dough.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        boolean hasPan    = dough.has(ModDataComponentTypes.PAN_TYPE.get());
        boolean hasProof  = dough.has(ModDataComponentTypes.PROOFING_STATE.get());
        boolean hasPct    = dough.has(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        boolean hasGrams  = dough.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        boolean hasRecipe = dough.has(ModDataComponentTypes.DOUGH_RECIPE.get());

        LOG.info("[DoughFactory] Dough → PROC:{} PAN:{} PROOF:{} PCT:{} GRAMS:{} RECIPE:{} (absStepOrdinal={}, procRawId={})",
                hasProc, hasPan, hasProof, hasPct, hasGrams, hasRecipe, stepOrdinal, procIdRaw);
    }

    public static ResourceLocation normalizeProcessId(ResourceLocation id) {
        String p = id.getPath();
        if (p.startsWith("dough_process/")) {
            return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), p.substring("dough_process/".length()));
        }
        return id;
    }

    private static void debugAssertBread(ItemStack bread, @Nullable ResourceLocation procIdRaw) {
        boolean hasProc   = bread.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        boolean hasPan    = bread.has(ModDataComponentTypes.PAN_TYPE.get());
        boolean hasType   = bread.has(ModDataComponentTypes.BREAD_TYPE.get());
        boolean hasRecipe = bread.has(ModDataComponentTypes.DOUGH_RECIPE.get());
        boolean hasPct    = bread.has(ModDataComponentTypes.BAKER_PERCENTAGES.get());
        boolean hasGrams  = bread.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());

        LOG.info("[DoughFactory] Bread → PROC:{} PAN:{} BREAD_TYPE:{} RECIPE:{} PCT:{} GRAMS:{} (procRawId={})",
                hasProc, hasPan, hasType, hasRecipe, hasPct, hasGrams, procIdRaw);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rounding / Adjustments
    // ─────────────────────────────────────────────────────────────────────

    private static List<IngredientInfo> roundNonAdditivesToGrams(List<IngredientInfo> src) {
        var out = new ArrayList<IngredientInfo>(src.size());
        for (IngredientInfo ii : src) {
            if (ii.category() == IngredientCategory.ADDITIVE) { out.add(ii); continue; }
            int roundedMg = Math.round(ii.milligrams() / 1000f) * 1000;
            if (roundedMg < 0) roundedMg = 0;
            IngredientInfo r = IngredientInfo.ofMg(ii.itemId(), ii.category(), roundedMg);
            if (ii.flourType() != null) r = r.withFlourType(ii.flourType());
            out.add(r);
        }
        return out;
    }

    private static List<IngredientInfo> adjustToServing(List<IngredientInfo> src, int targetGrams) {
        int targetMg = targetGrams * 1000;
        int sum = src.stream().mapToInt(IngredientInfo::milligrams).sum();
        int delta = targetMg - sum;
        if (delta == 0) return src;

        int idx = -1;
        for (int i = 0; i < src.size(); i++) if (src.get(i).category() == IngredientCategory.WATER) { idx = i; break; }
        if (idx == -1) for (int i = 0; i < src.size(); i++) if (src.get(i).category() != IngredientCategory.ADDITIVE) { idx = i; break; }
        if (idx == -1) return src;

        var out = new ArrayList<IngredientInfo>(src);
        IngredientInfo base = out.get(idx);
        int newMg = Math.max(0, base.milligrams() + delta);
        IngredientInfo adj = IngredientInfo.ofMg(base.itemId(), base.category(), newMg);
        if (base.flourType() != null) adj = adj.withFlourType(base.flourType());
        out.set(idx, adj);
        return out;
    }

    public static BuildResult buildIngredientInfosFromRatio(
            RatioRecipe rr,
            Map<IngredientCategory, Double> pctByCat,
            @Nullable DoughProcessRecipe proc,
            int servings,
            boolean humanize
    ) {
        servings = Math.max(1, servings);

        // Serving basis (prefer process -> ratio -> default 454g)
        double perServing = safeServingWeight(proc, rr);

        // Flour basis math
        double nonFlourPct = pctByCat.entrySet().stream()
                .filter(e -> e.getKey() != IngredientCategory.FLOUR)
                .mapToDouble(Map.Entry::getValue).sum();

        double totalTargetGrams = perServing * servings;
        double flourGrams = totalTargetGrams / (1.0 + nonFlourPct / 100.0);

        LOG.info("[DoughFactory] basis → perServing={}g, servings={}, nonFlourPct={}, flourBasis={}g",
                String.format(java.util.Locale.ROOT, "%.3f", perServing),
                servings,
                String.format(java.util.Locale.ROOT, "%.3f", nonFlourPct),
                String.format(java.util.Locale.ROOT, "%.3f", flourGrams));

        List<?> comps = getRatioComponents(rr);
        List<IngredientInfo> out = new ArrayList<>();
        int totalMg = 0;

        if (comps != null) {
            for (Object raw : comps) {
                DetectedRatioComponent det = detRatioComponent(raw);
                if (det == null) continue;

                // grams → mg (precise, with rounding to the nearest mg)
                double gramsExact = flourGrams * (det.percent() / 100.0);
                int mg = (int) Math.round(gramsExact * 1000.0);

                ResourceLocation itemId = det.allowedIds().isEmpty()
                        ? defaultItemForCategory(det.category())
                        : det.allowedIds().get(0);

                LOG.info("[DoughFactory]   comp cat={} pct={} → {} mg ({} g) item={}",
                        det.category(),
                        String.format(java.util.Locale.ROOT, "%.3f", det.percent()),
                        mg,
                        String.format(java.util.Locale.ROOT, "%.3f", mg / 1000.0),
                        itemId);

                IngredientInfo info = IngredientInfo.ofMg(itemId.toString(), det.category(), mg);
                out.add(info);
                totalMg += mg;
            }
        }

        // Optionally “humanize” (round non-additives to whole grams) then adjust one ingredient to keep the total
        if (humanize) {
            out = roundNonAdditivesToGrams(out);
            out = adjustToServing(out, (int) Math.round(perServing * servings));
            totalMg = out.stream().mapToInt(IngredientInfo::milligrams).sum();
        }

        int totalGramsRounded = Math.round(totalMg / 1000f);

        LOG.info("[DoughFactory] total = {} mg ({} g rounded)", totalMg, totalGramsRounded);

        return new BuildResult(out, totalMg, totalGramsRounded);
    }

    // Handy if you want the exact same key order everywhere
    public static LinkedHashMap<IngredientCategory, Double> canonicalizeTargets(Map<IngredientCategory, Double> in) {
        LinkedHashMap<IngredientCategory, Double> out = new LinkedHashMap<>();
        Arrays.stream(IngredientCategory.values()).forEach(cat -> {
            Double v = in.get(cat);
            if (v != null) out.put(cat, v);
        });
        return out;
    }
}
