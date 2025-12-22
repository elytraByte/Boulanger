package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.*;
import net.boulangermod.boulanger.util.DoughRecipeCanonicalier;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.IngredientStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Encapsulates ingredient tracking and dough generation for mixer blocks.
 */
public class MixerState {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<IngredientCategory, Double> preciseTotalsG = new EnumMap<>(IngredientCategory.class);
    private final List<IngredientStack> ingredientList = new ArrayList<>();

    /* ────────────────────────────── accessors ───────────────────────────── */

    public List<IngredientStack> getIngredientList() {
        return ingredientList;
    }

    public Map<IngredientCategory, Double> getPreciseTotalsG() {
        return preciseTotalsG;
    }

    public void clear() {
        ingredientList.clear();
        preciseTotalsG.clear();
    }

    /** Check if the stack is a weighed ingredient bowl. */
    public static boolean isWeighedIngredient(ItemStack s) {
        return s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
    }

    public void addIngredientFromBowl(ItemStack bowl) {
        IngredientCategory cat = bowl.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                ? bowl.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                : null;
        if (cat == null) return;

        int grams = bowl.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())
                ? Math.max(0, Math.round(bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).grams()))
                : 0;
        int mg = grams * 1000;

        Item ingItem = null;

        if (cat == IngredientCategory.FLOUR) {
            FlourType ft = bowl.get(ModDataComponentTypes.FLOUR_TYPE.get());
            if (ft != null) {
                // display FLOUR with our flour item and keep FlourType for matching
                ingredientList.add(new IngredientStack(ModItems.FLOUR_ITEM.get(), IngredientCategory.FLOUR, ft, mg));
                preciseTotalsG.merge(IngredientCategory.FLOUR, grams * 1.0, Double::sum);
                return;
            }
        }

        var itc = bowl.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (itc != null) {
            ingItem = itc.item();
        } else if (bowl.has(ModDataComponentTypes.FLOUR_TYPE.get())) {
            // fall back for older bowls
            FlourType ft = bowl.get(ModDataComponentTypes.FLOUR_TYPE.get());
            ingredientList.add(new IngredientStack(ModItems.FLOUR_ITEM.get(),
                    IngredientCategory.FLOUR, ft, mg));
            preciseTotalsG.merge(IngredientCategory.FLOUR, grams * 1.0, Double::sum);
            return;
        } else {
            ingItem = BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getDefaultKey());
        }

        ingredientList.add(new IngredientStack(ingItem, cat, null, mg));
        preciseTotalsG.merge(cat, grams * 1.0, Double::sum);
    }

    @Nullable
    public IngredientStack popIngredient() {
        if (ingredientList.isEmpty()) return null;
        IngredientStack st = ingredientList.remove(ingredientList.size() - 1);
        preciseTotalsG.merge(st.getCategory(), -st.getMilligrams() / 1000.0, Double::sum);
        return st;
    }

    /* ───────────────────── recipe matching & canonicalization ───────────── */

    private @Nullable ResourceLocation ingredientIdFromBowl(ItemStack bowl, IngredientCategory cat) {
        if (cat == IngredientCategory.FLOUR) {
            FlourType ft = bowl.get(ModDataComponentTypes.FLOUR_TYPE.get());
            if (ft != null) {
                return ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, ft.getId());
            }
            return null;
        }

        var itc = bowl.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (itc != null) {
            return BuiltInRegistries.ITEM.getKey(itc.item());
        }
        return null;
    }

    public Optional<RatioRecipe> findMatchingRecipe(Level level) {
        Map<IngredientCategory, Integer> totalsMg = new EnumMap<>(IngredientCategory.class);
        Map<IngredientCategory, Map<ResourceLocation, Integer>> perIdMg = new EnumMap<>(IngredientCategory.class);

        for (IngredientStack st : ingredientList) {
            int mg = Math.max(0, st.getMilligrams());
            if (mg == 0) continue;

            IngredientCategory cat = st.getCategory();
            totalsMg.merge(cat, mg, Integer::sum);

            ResourceLocation rid = canonicalIdForMatching(st);
            perIdMg.computeIfAbsent(cat, k -> new HashMap<>()).merge(rid, mg, Integer::sum);
        }

        int flourMg = totalsMg.getOrDefault(IngredientCategory.FLOUR, 0);
        if (flourMg <= 0) return Optional.empty();

        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializers.RATIO_TYPE.get());
        for (var holder : recipes) {
            RatioRecipe recipe = holder.value();

            boolean ok = true;
            double tolPctPoints = recipe.getTolerance();

            for (IngredientComponent comp : recipe.getComponents()) {
                IngredientCategory cat = comp.category();
                double targetPct = comp.targetPercent();
                List<ResourceLocation> allowedIds = comp.allowedItems();

                int gotMgInt;
                if (allowedIds == null || allowedIds.isEmpty()) {
                    gotMgInt = totalsMg.getOrDefault(cat, 0);
                } else {
                    int sum = 0;
                    Map<ResourceLocation, Integer> byId = perIdMg.get(cat);
                    if (byId != null) {
                        for (ResourceLocation a : allowedIds) sum += byId.getOrDefault(a, 0);
                    }
                    gotMgInt = sum;
                }

                double gotPct = flourMg == 0 ? 0.0 : (gotMgInt * 100.0) / flourMg;
                double deltaPct = Math.abs(gotPct - targetPct);

                if (deltaPct > tolPctPoints + 1e-9) {
                    ok = false;
                    break;
                }
            }

            if (ok) return Optional.of(recipe);
        }
        return Optional.empty();
    }

    private static ResourceLocation canonicalIdForMatching(IngredientStack st) {
        if (st.getCategory() == IngredientCategory.FLOUR) {
            FlourType ft = st.getFlourType();
            if (ft != null) {
                String id = ft.getId();
                return (id.indexOf(':') >= 0)
                        ? ResourceLocation.parse(id)
                        : ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, id);
            }
            return BuiltInRegistries.ITEM.getKey(st.getActualItem());
        }
        return BuiltInRegistries.ITEM.getKey(st.getActualItem());
    }

    /* ─────────────────────────── dough generation ───────────────────────── */

    public ItemStack generateDough(Level level, double maxDoughWeightGrams) {
        Optional<RatioRecipe> optRatio = findMatchingRecipe(level);
        if (optRatio.isEmpty()) {
            ingredientList.clear();
            preciseTotalsG.clear();
            return ItemStack.EMPTY;
        }

        RatioRecipe ratio = optRatio.get();

        // --- resolve distinct process id (prefer explicit getter; else "<base>_process")
        ResourceLocation processId = null;
        try {
            var m = RatioRecipe.class.getMethod("getProcessId");
            Object pid = m.invoke(ratio);
            if (pid instanceof ResourceLocation rl) processId = rl;
            else if (pid instanceof String s) processId = ResourceLocation.parse(s);
        } catch (Throwable ignored) {}
        if (processId == null) {
            processId = ResourceLocation.fromNamespaceAndPath(
                    ratio.getId().getNamespace(), ratio.getId().getPath() + "_process");
        }

        // --- base dough stack (fallback to generic dough)
        ItemStack dough = ratio.getResultItem(null).copy();
        if (dough.isEmpty()) dough = new ItemStack(ModItems.DOUGH.get());

        // --- baker % target (group components by category)
        Map<IngredientCategory, Double> targetMap = ratio.getComponents().stream()
                .collect(Collectors.groupingBy(
                        IngredientComponent::category,
                        Collectors.summingDouble(IngredientComponent::targetPercent)
                ));
        dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), new BakerPctComponent(targetMap));

        // --- build canonical ingredient list (prefer FlourType id for FLOUR)
        List<DoughRecipeCanonicalier.Ingredient> canonInputs = new ArrayList<>();
        for (IngredientStack st : ingredientList) {
            int mg = Math.max(0, st.getMilligrams());
            if (mg <= 0) continue;

            IngredientCategory catEnum = st.getCategory();
            ResourceLocation itemId;

            if (catEnum == IngredientCategory.FLOUR) {
                FlourType ft = st.getFlourType();
                if (ft != null) {
                    String idStr = ft.getId();
                    itemId = (idStr.indexOf(':') >= 0)
                            ? ResourceLocation.parse(idStr)
                            : ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, idStr);
                } else {
                    itemId = BuiltInRegistries.ITEM.getKey(st.getActualItem());
                    LOGGER.warn("Flour stack missing FlourType; falling back to {}", itemId);
                }
            } else {
                itemId = BuiltInRegistries.ITEM.getKey(st.getActualItem());
            }

            canonInputs.add(new DoughRecipeCanonicalier.Ingredient(catEnum.name(), itemId, mg));
        }

        // --- canonicalize whole dough recipe payload
        int totalMg = canonInputs.stream().mapToInt(DoughRecipeCanonicalier.Ingredient::milligrams).sum();

        // convert targetMap<IngredientCategory,Double> -> <String,Double> for the canonicalizer
        Map<String, Double> targetPctByString = new LinkedHashMap<>();
        for (var e : targetMap.entrySet()) targetPctByString.put(e.getKey().name(), e.getValue());

        var canon = DoughRecipeCanonicalier.canonicalizeFromPieces(
                ratio.getId(),               // keep the *base* ratio id in the canonical recipe
                Math.max(0, totalMg / 1000), // total grams (int)
                targetPctByString,
                canonInputs
        );

        List<IngredientInfo> infos = toIngredientInfosFromCanon(canon);

        dough.set(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                DoughRecipeComponent.of(
                        ratio.getId(),
                        infos,
                        Math.max(0, totalMg / 1000),  // <-- totalWeight (grams)
                        targetMap                     // <-- targetPercentages
                )
        );


        // pin process id (distinct from ratio id)
        dough.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), processId);

        try {
            var getDefaultPan = RatioRecipe.class.getMethod("getDefaultPanType");
            Object pan = getDefaultPan.invoke(ratio);
            if (pan instanceof PanType pt) {
                dough.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(pt.getId()));
            }
        } catch (Throwable ignored) {}

        // --- final cleanup
        ingredientList.clear();
        preciseTotalsG.clear();

        return dough;
    }

    private static List<IngredientInfo> toIngredientInfosFromCanon(DoughRecipeCanonicalier.DoughRecipe canon) {
        List<IngredientInfo> result = new ArrayList<>();
        for (var ing : canon.ingredients()) {
            IngredientCategory cat;
            try {
                cat = IngredientCategory.valueOf(ing.category());
            } catch (IllegalArgumentException e) {
                cat = IngredientCategory.ADDITIVE;
            }

            int mg = Math.max(0, ing.milligrams());
            ResourceLocation id = ing.itemId();

            if (cat == IngredientCategory.FLOUR) {
                // Prefer FlourType-style ids; strip namespace if it's ours
                String flourId = Boulanger.MODID.equals(id.getNamespace()) ? id.getPath() : id.toString();
                result.add(IngredientInfo.ofMg(flourId, cat, mg));
            } else {
                // <-- use id here, not flourId
                result.add(IngredientInfo.ofMg(id.toString(), cat, mg)); // or `id` if your ofMg accepts ResourceLocation
            }
        }
        return result;
    }


    /* ───────────────────────── display helpers for UI ───────────────────── */
    /** Build a stack to render for this ingredient, with any identifying components attached. */
    public ItemStack toDisplayStack(IngredientStack st) {
        ItemStack stack = new ItemStack(st.getActualItem());
        if (st.getCategory() == IngredientCategory.FLOUR) {
            FlourType ft = st.getFlourType();
            if (ft != null) {
                stack.set(ModDataComponentTypes.FLOUR_TYPE.get(), ft);
            }
        }
        return stack;
    }

    /** Convenience: prebuild stacks for the sidebar UI. Indices match getIngredientList(). */
    public java.util.List<ItemStack> buildDisplayStacks() {
        java.util.ArrayList<ItemStack> out = new java.util.ArrayList<>(ingredientList.size());
        for (IngredientStack st : ingredientList) out.add(toDisplayStack(st));
        return out;
    }

    /* ───────────────────────────── NBT helpers ──────────────────────────── */

    // NBT keys
    private static final String NBT_CAT   = "category";
    private static final String NBT_ITEM  = "item";
    private static final String NBT_MG    = "mg";
    private static final String NBT_FLOUR = "flour"; // only for FLOUR

    public ListTag saveIngredientList() {
        ListTag list = new ListTag();

        for (IngredientStack st : ingredientList) {
            CompoundTag t = new CompoundTag();

            t.putString(NBT_CAT, st.getCategory().name());
            if (st.getCategory() == IngredientCategory.FLOUR) {
                t.putString(NBT_ITEM, BuiltInRegistries.ITEM.getKey(ModItems.FLOUR_ITEM.get()).toString());
            } else {
                t.putString(NBT_ITEM, BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString());
            }
            t.putInt(NBT_MG, Math.max(0, st.getMilligrams()));

            if (st.getCategory() == IngredientCategory.FLOUR) {
                FlourType ft = st.getFlourType();
                if (ft != null) t.putString(NBT_FLOUR, ft.getId());
            }

            list.add(t);
        }

        return list;
    }

    public void loadIngredientList(ListTag list) {
        ingredientList.clear();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);

            IngredientCategory cat;
            try { cat = IngredientCategory.valueOf(t.getString(NBT_CAT)); }
            catch (IllegalArgumentException ex) { cat = IngredientCategory.ADDITIVE; }

            Item item;
            if (cat == IngredientCategory.FLOUR) {
                item = ModItems.FLOUR_ITEM.get();
            } else {
                item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(t.getString(NBT_ITEM)));
            }
            int mg = Math.max(0, t.getInt(NBT_MG));

            FlourType ft = null;
            if (cat == IngredientCategory.FLOUR && t.contains(NBT_FLOUR, Tag.TAG_STRING)) {
                ft = new FlourType(t.getString(NBT_FLOUR), 0f, 0f, 0, 0f); // or your real lookup
            }

            // ctor order: (item, cat, ft, mg)
            ingredientList.add(new IngredientStack(item, cat, ft, mg));
        }
    }


    public CompoundTag savePreciseTotals() {
        CompoundTag tag = new CompoundTag();
        for (var e : preciseTotalsG.entrySet()) {
            tag.putDouble(e.getKey().name(), e.getValue());
        }
        return tag;
    }

    public void loadPreciseTotals(@Nullable CompoundTag tag) {
        preciseTotalsG.clear();
        if (tag != null) {
            for (IngredientCategory c : IngredientCategory.values()) {
                if (tag.contains(c.name())) {
                    preciseTotalsG.put(c, tag.getDouble(c.name()));
                }
            }
        } else {
            for (IngredientStack st : ingredientList) {
                preciseTotalsG.merge(st.getCategory(), st.getMilligrams() / 1000.0, Double::sum);
            }
        }
    }
}
