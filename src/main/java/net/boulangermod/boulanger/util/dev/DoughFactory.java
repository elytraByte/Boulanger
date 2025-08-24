// src/main/java/net/boulangermod/boulanger/util/dev/DoughFactory.java
package net.boulangermod.boulanger.util.dev;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.recipe.StepType;
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

    /** Legacy enum kept only so old code that parsed strings still compiles. Not used by commands. */
    public enum Stage { MIXED, PROOFED, FINAL_PROOFED, BAKED;
        public static Stage fromString(@Nullable String s) {
            if (s == null) return FINAL_PROOFED;
            switch (s.toLowerCase(Locale.ROOT)) {
                case "mixed": return MIXED;
                case "proofed": return PROOFED;
                case "final_proofed":
                case "final-proofed":
                case "final":
                case "finalproofed": return FINAL_PROOFED;
                case "baked": return BAKED;
                default: return FINAL_PROOFED;
            }
        }
    }

    /** Set true to always stamp PAN_TYPE on dough (even pre-shape) for model testing. */
    private static final boolean DEV_ALWAYS_SET_PAN_ON_DOUGH = false;

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

        // 0) Link process (serving size / pan / steps). Write NORMALIZED id to the item.
        ResourceLocation procIdRaw = findLinkedDoughProcessId(level, ratioId);
        DoughProcessRecipe proc = null;
        if (procIdRaw != null) {
            dough.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), normalizeProcessId(procIdRaw));
            var holder = rm.byKey(procIdRaw); // use raw to fetch (registered with full "dough_process/..." path)
            if (holder.isPresent() && holder.get().value() instanceof DoughProcessRecipe p) {
                proc = p;
            }
        } else {
            LOG.warn("No DoughProcessRecipe linked to ratio {}. (dough_type mismatch?)", ratioId);
        }

        // 1) Baker’s % map (display / math)
        Map<IngredientCategory, Double> pctByCat = getPctByCategory(rr);
        trySetBakerPct(dough, pctByCat);

        // 2) Concrete ingredient list (rounded grams) + total grams
        BuildResult built = buildIngredientInfos(rr, pctByCat, proc, Math.max(1, servings));

        // 3) Total weight
        dough.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), WeightComponent.ofGrams(built.totalGrams()));

        // 4) DoughRecipe component (provenance + targets + concrete ingredients)
        dough.set(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(ratioId, pctByCat, built.ingredients(), built.totalGrams())
        );

        // 5) Proofing state from ABSOLUTE ordinal — start at this step, ticks=0, NEVER pre-shaped.
        if (proc != null) {
            ProofingStateComponent st = computeStateByAbsoluteStep(proc, stepOrdinal);
            dough.set(ModDataComponentTypes.PROOFING_STATE.get(), st);

            // Do NOT stamp pan on dough at spawn; shaping/panning happens at the Baker's Table.
            if (DEV_ALWAYS_SET_PAN_ON_DOUGH) {
                ResourceLocation pan = proc.getPanType();
                if (pan != null) {
                    dough.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(pan.toString()));
                }
            }

            // Defensive: ensure no pan is present (e.g., if a normalizer stamped it).
            try {
                if (dough.has(ModDataComponentTypes.PAN_TYPE.get())) {
                    dough.remove(ModDataComponentTypes.PAN_TYPE.get());
                }
            } catch (Throwable ignored) {
                try { dough.set(ModDataComponentTypes.PAN_TYPE.get(), null); } catch (Throwable ignored2) {}
            }

            // Defensive: re-force shaped=false in case a normalizer flipped it after set.
            try {
                var current = dough.get(ModDataComponentTypes.PROOFING_STATE.get());
                if (current != null) {
                    dough.set(ModDataComponentTypes.PROOFING_STATE.get(), forceShaped(current, false));
                }
            } catch (Throwable t) {
                LOG.warn("[DoughFactory] Failed to force shaped=false: {}", t.toString());
            }
        } else {
            // No process found: set to finalProofed so machines still accept it
            dough.set(ModDataComponentTypes.PROOFING_STATE.get(), ProofingStateComponent.finalProofed());
        }

        debugAssertDough(dough, stepOrdinal, procIdRaw);
        return dough;
    }

    /**
     * Create a bread item from a RatioRecipe.
     * Writes NORMALIZED process id; sets PAN_TYPE and BREAD_TYPE; also carries baker % & grams like dough for tooltips.
     */
    public static ItemStack createBreadFromRatio(ServerLevel level, ResourceLocation ratioId, int servings) {
        var holderOpt = level.getRecipeManager().byKey(ratioId);
        if (holderOpt.isEmpty() || !(holderOpt.get().value() instanceof RatioRecipe rr)) {
            LOG.warn("No RatioRecipe found for {}", ratioId);
            return ItemStack.EMPTY;
        }

        ItemStack bread = new ItemStack(ModItems.BREAD.get());

        // Compute the same math the dough had so bread tooltips look rich
        ResourceLocation procIdRaw = findLinkedDoughProcessId(level, ratioId);
        DoughProcessRecipe proc = null;
        if (procIdRaw != null) {
            bread.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), normalizeProcessId(procIdRaw)); // normalize on item
            var holder = level.getRecipeManager().byKey(procIdRaw);
            if (holder.isPresent() && holder.get().value() instanceof DoughProcessRecipe p) {
                proc = p;
            }
        }

        Map<IngredientCategory, Double> pctByCat = getPctByCategory(rr);
        trySetBakerPct(bread, pctByCat);

        BuildResult built = buildIngredientInfos(rr, pctByCat, proc, Math.max(1, servings));
        bread.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), WeightComponent.ofGrams(built.totalGrams()));

        bread.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(ratioId, pctByCat, built.ingredients(), built.totalGrams()));

        if (proc != null) {
            ResourceLocation pan = proc.getPanType();
            if (pan != null) {
                bread.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(pan.toString()));
                BreadType bt = breadTypeForPan(pan);
                if (bt != null) bread.set(ModDataComponentTypes.BREAD_TYPE.get(), bt);
            }
        } else {
            LOG.warn("No DoughProcessRecipe linked to ratio {}. Bread will miss PAN/BREAD_TYPE.", ratioId);
        }

        debugAssertBread(bread, procIdRaw);
        return bread;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ingredient construction
    // ─────────────────────────────────────────────────────────────────────

    private record BuildResult(List<IngredientInfo> ingredients, int totalGrams) {}

    private static BuildResult buildIngredientInfos(RatioRecipe rr,
                                                    Map<IngredientCategory, Double> pctByCat,
                                                    @Nullable DoughProcessRecipe proc,
                                                    int servings) {
        servings = Math.max(1, servings);

        // Serving basis (prefer process -> ratio -> default 454g)
        double perServing = safeServingWeight(proc, rr);

        // Flour basis math
        double nonFlourPct = pctByCat.entrySet().stream()
                .filter(e -> e.getKey() != IngredientCategory.FLOUR)
                .mapToDouble(Map.Entry::getValue).sum();

        double totalTarget = perServing * servings;
        double flourGrams  = totalTarget / (1.0 + nonFlourPct / 100.0);

        LOG.info("[DoughFactory] basis → perServing={}g, servings={}, nonFlourPct={}, flourBasis={}g",
                perServing, servings, nonFlourPct, flourGrams);

        List<?> comps = getRatioComponents(rr);
        List<IngredientInfo> out = new ArrayList<>();
        int total = 0;

        if (comps != null) {
            for (Object raw : comps) {
                DetectedRatioComponent det = detRatioComponent(raw);
                if (det == null) continue;

                int grams = (int) Math.round(flourGrams * (det.percent() / 100.0));

                ResourceLocation itemId = det.allowedIds().isEmpty()
                        ? defaultItemForCategory(det.category())
                        : det.allowedIds().get(0);

                LOG.info("[DoughFactory]   comp cat={} pct={} → grams={} item={}",
                        det.category(), det.percent(), grams, itemId);

                IngredientInfo info = new IngredientInfo(itemId.toString(), det.category(), grams, null);
                out.add(info);
                total += grams;
            }
        }

        LOG.info("[DoughFactory] total grams (rounded sum) = {}", total);
        return new BuildResult(out, total);
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

    /**
     * Build a ProofingStateComponent for the absolute process step.
     * - Clamps to [0..N-1]
     * - Sets ticks_in_step = 0 (so it starts at this step, NOT auto-advancing)
     * - NEVER pre-shaped on spawn
     */
    private static ProofingStateComponent computeStateByAbsoluteStep(DoughProcessRecipe proc, int stepOrdinal) {
        List<?> steps = proc.getSteps();
        if (steps == null || steps.isEmpty()) {
            return ProofingStateComponent.finalProofed();
        }
        int idx = Math.max(0, Math.min(stepOrdinal - 1, steps.size() - 1)); // 1..N -> 0..N-1
        // Create the instance with shaped=false and ticks=0
        ProofingStateComponent st = new ProofingStateComponent(idx, /*ticks_in_step=*/0, /*shaped=*/false);
        // If the component (or its canonical ctor) auto-derives shaped, override it:
        return forceShaped(st, /*value=*/false);
    }

    private static ProofingStateComponent forceShaped(ProofingStateComponent st, boolean value) {
        // 1) Prefer a copy/with method if your component provides one
        try {
            var m = st.getClass().getMethod("withShaped", boolean.class);
            Object out = m.invoke(st, value);
            return (ProofingStateComponent) out;
        } catch (Throwable ignored) {}

        // 2) Try a canonical/“of” factory with (idx, ticks, shaped)
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

        // 3) Last resort: set the field reflectively (works in dev; avoid in prod)
        try {
            var f = st.getClass().getDeclaredField("shaped");
            f.setAccessible(true);
            f.setBoolean(st, value);
            return st;
        } catch (Throwable ignored) {}

        // If all else fails, return as-is (but log so you can chase the root cause)
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

    /** Get a step's duration (in ticks). Looks for common names; falls back to 1600. */
    private static int stepDurationTicks(Object step) {
        Number n = (Number) callAny(step, new String[]{
                "ticks", "getTicks", "duration", "getDuration", "durationTicks", "getDurationTicks", "time", "getTime"
        });
        if (n != null) return Math.max(0, n.intValue());
        // field scan
        try {
            for (String name : new String[]{"ticks", "duration", "durationTicks", "time"}) {
                Field f = step.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(step);
                if (v instanceof Number nn) return Math.max(0, nn.intValue());
            }
        } catch (Throwable ignored) {}
        // safe default: matches your data gen
        return 1600;
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

    /** Reflect a single ratio component object into {category, percent, allowedIds}. */
    @Nullable
    private static DetectedRatioComponent detRatioComponent(Object comp) {
        if (comp == null) return null;

        // CATEGORY
        IngredientCategory cat = (IngredientCategory) callAny(comp, new String[]{"category", "getCategory"});
        if (cat == null) return null;

        // PERCENT (method first, then field scan)
        Number pctNum = (Number) callAny(comp, new String[]{
                "percent", "getPercent", "percentage", "getPercentage",
                "bakersPercent", "getBakersPercent", "value", "getValue", "ratio", "getRatio"
        });
        if (pctNum == null) pctNum = firstNumericField(comp);
        double pct = pctNum == null ? 0.0 : pctNum.doubleValue();

        // ALLOWED IDS (method first, then field scan)
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

    /** Pull the ratio's component list: supports both record-style `components()` and bean-style `getComponents()`. */
    @Nullable
    private static List<?> getRatioComponents(RatioRecipe rr) {
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

    /** First allowed item id taken from a component (via detRatioComponent). */
    @Nullable
    private static ResourceLocation firstAllowedId(Object comp) {
        DetectedRatioComponent det = detRatioComponent(comp);
        if (det == null || det.allowedIds().isEmpty()) return null;
        return det.allowedIds().get(0);
    }

    /** Fallback when a component provides no explicit allowed item ids. Adjust these to your registry ids. */
    private static ResourceLocation defaultItemForCategory(IngredientCategory cat) {
        switch (cat) {
            case WATER:  return ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket");
            case YEAST:  return ResourceLocation.fromNamespaceAndPath("boulanger", "fleischmann");
            case SALT:   return ResourceLocation.fromNamespaceAndPath("boulanger", "salt_kosher");
            case FLOUR:  return ResourceLocation.fromNamespaceAndPath("boulanger", "bread_flour");
            case DAIRY:  return ResourceLocation.fromNamespaceAndPath("boulanger", "whole_milk");
            case SUGAR:  return ResourceLocation.fromNamespaceAndPath("boulanger", "brown_sugar");
            case EGGS:   return ResourceLocation.fromNamespaceAndPath("boulanger", "fancy_egg");
            case FAT:    return ResourceLocation.fromNamespaceAndPath("boulanger", "unsalted_butter");
            default:     return ResourceLocation.fromNamespaceAndPath("minecraft", "air");
        }
    }

    /** Try a list-like object and return the first element parsed as ResourceLocation. Prefers your modid if none given. */
    @Nullable
    private static ResourceLocation firstRLFromAny(Object o) {
        if (o instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof ResourceLocation rl) return rl;
            if (first instanceof String s) {
                if (s.indexOf(':') < 0) { // prefix our modid if missing
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

    /** Find the DoughProcessRecipe id linked to the given RatioRecipe id via doughType. */
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
//        if (p.contains("loaf"))     return BreadType.LOAF;
//        if (p.contains("boule"))    return BreadType.BOULE;
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Baker’s math + extractors
    // ─────────────────────────────────────────────────────────────────────

    private static Map<IngredientCategory, Double> getPctByCategory(RatioRecipe rr) {
        // 1) Typed: Map<IngredientCategory, Double> percentByCategory()
        try {
            Method m = rr.getClass().getMethod("percentByCategory");
            @SuppressWarnings("unchecked")
            Map<IngredientCategory, Double> typed = (Map<IngredientCategory, Double>) m.invoke(rr);
            if (typed != null && !typed.isEmpty()) return new EnumMap<>(typed);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) { e.printStackTrace(); }

        // 2) Reflect: components()/getComponents()
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

        // 3) Empty default
        return new EnumMap<>(IngredientCategory.class);
    }

    private static double getServingWeight(RatioRecipe rr) {
        // Try methods
        Number n = (Number) callAny(rr, new String[]{
                "getServingWeight", "servingWeight", "getServingWeightGrams", "servingWeightGrams"
        });
        if (n != null) return n.doubleValue();
        // Try fields (record case)
        try {
            for (String fName : new String[]{"servingWeight", "servingWeightGrams"}) {
                Field f = rr.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object v = f.get(rr);
                if (v instanceof Number nn) return nn.doubleValue();
            }
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
}
