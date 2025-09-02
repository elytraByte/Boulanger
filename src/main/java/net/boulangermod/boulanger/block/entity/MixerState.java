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

    public List<IngredientStack> getIngredientList() {
        return Collections.unmodifiableList(ingredientList);
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
                : IngredientCategory.getIngredientCategory(bowl);

        WeightComponent wc = bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (wc == null) return;
        int mg = Math.max(0, Math.round(wc.grams() * 1000f));
        if (mg <= 0) return;
        double g = mg / 1000.0;

        if (cat == IngredientCategory.FLOUR) {
            FlourType ft = bowl.get(ModDataComponentTypes.FLOUR_TYPE.get());
            Item flourItem = ModItems.FLOUR_ITEM.get();
            ingredientList.add(new IngredientStack(flourItem, IngredientCategory.FLOUR, ft, mg));
            preciseTotalsG.merge(cat, g, Double::sum);
            return;
        }

        ResourceLocation bowlIngId = ingredientIdFromBowl(bowl, cat);
        Item ingItem = (bowlIngId != null) ? BuiltInRegistries.ITEM.get(bowlIngId) : bowl.getItem();

        ingredientList.add(new IngredientStack(ingItem, cat, null, mg));
        preciseTotalsG.merge(cat, g, Double::sum);
    }

    private ResourceLocation ingredientIdFromBowl(ItemStack bowl, IngredientCategory cat) {
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
            double tolPctPoints = recipe.getTolerance() * 100.0;

            boolean ok = true;
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

            if (ok) {
                return Optional.of(recipe);
            }
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

    public ItemStack generateDough(Level level, double maxDoughWeightGrams) {
        Optional<RatioRecipe> optRatio = findMatchingRecipe(level);
        if (optRatio.isEmpty()) {
            ingredientList.clear();
            preciseTotalsG.clear();
            return ItemStack.EMPTY;
        }
        RatioRecipe ratio = optRatio.get();

        ItemStack dough = new ItemStack(ModItems.DOUGH.get());

        Map<IngredientCategory, Double> targetMap = ratio.getComponents().stream()
                .collect(Collectors.groupingBy(
                        IngredientComponent::category,
                        Collectors.summingDouble(IngredientComponent::targetPercent)
                ));
        dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), new BakerPctComponent(targetMap));

        List<DoughRecipeCanonicalier.Ingredient> canonInputs = new ArrayList<>();
        Map<String, FlourType> flourTypeByKey = new HashMap<>();
        Map<ResourceLocation, Integer> debugFlourPerIdMg = new HashMap<>();

        int totalMgUncanonical = 0;

        for (IngredientStack st : ingredientList) {
            final IngredientCategory catEnum = st.getCategory();
            final String cat = catEnum.name();
            final int mg = Math.max(0, st.getMilligrams());

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

            canonInputs.add(new DoughRecipeCanonicalier.Ingredient(cat, itemId, mg));
            totalMgUncanonical += mg;

            if (catEnum == IngredientCategory.FLOUR) {
                FlourType ft = st.getFlourType();
                if (ft != null) {
                    flourTypeByKey.put(cat + "|" + itemId.toString(), ft);
                }
                debugFlourPerIdMg.merge(itemId, mg, Integer::sum);
            }
        }

        if (!debugFlourPerIdMg.isEmpty()) {
            String dbg = debugFlourPerIdMg.entrySet().stream()
                    .map(e -> e.getKey() + "=" + (e.getValue() / 1000.0) + "g")
                    .collect(Collectors.joining(", "));
            LOGGER.debug("FLOUR perId (canonical): {}", dbg);
        }

        List<String> pctKeys = new ArrayList<>();
        List<Double> pctVals = new ArrayList<>();
        targetMap.forEach((k, v) -> { pctKeys.add(k.name()); pctVals.add(v); });

        int totalGramsRounded = Math.round(totalMgUncanonical / 1000f);
        DoughRecipeCanonicalier.DoughRecipe canonIn = new DoughRecipeCanonicalier.DoughRecipe(
                ratio.getId(),
                totalGramsRounded,
                pctKeys,
                pctVals,
                canonInputs
        );
        DoughRecipeCanonicalier.DoughRecipe canonOut = DoughRecipeCanonicalier.canonicalize(canonIn);

        List<IngredientInfo> infos = new ArrayList<>(canonOut.ingredients().size());
        int totalMg = 0;
        for (DoughRecipeCanonicalier.Ingredient ing : canonOut.ingredients()) {
            IngredientCategory cat = IngredientCategory.valueOf(ing.category());
            String itemIdStr = ing.itemId().toString();

            IngredientInfo info = IngredientInfo.ofMg(itemIdStr, cat, ing.milligrams());
            if (cat == IngredientCategory.FLOUR) {
                String key = ing.category() + "|" + ing.itemId().toString();
                FlourType ft = flourTypeByKey.get(key);
                if (ft != null) {
                    info = info.withFlourType(ft);
                } else {
                    LOGGER.warn("Missing FlourType reattachment for {}", key);
                }
            }
            infos.add(info);
            totalMg += ing.milligrams();
        }

        dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                new DoughRecipeComponent(ratio.getId(), targetMap, infos, totalGramsRounded));
        dough.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                new WeightComponent((float) totalGramsRounded));

        dough.set(ModDataComponentTypes.PROOFING_STATE.get(),
                new ProofingStateComponent(0, 0, false));
        dough.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), ratio.getId());

        Optional<DoughProcessRecipe> optProcess = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(p -> p.getDoughType().equals(ratio.getId()))
                .findFirst();

        if (optProcess.isPresent()) {
            ResourceLocation panLoc = optProcess.get().getPanType();
            if (panLoc != null) {
                PanType panEnum = PanType.byId(panLoc);
                if (panEnum != null) {
                    dough.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(panEnum.getId()));
                } else {
                    LOGGER.warn("Unknown pan type '{}' for recipe {}", panLoc, ratio.getId());
                }
            }
        } else {
            LOGGER.warn("⚠ No DoughProcessRecipe found for dough type: {}", ratio.getId());
        }

        double totalGramsExact = totalMg / 1000.0;
        if (totalGramsExact > maxDoughWeightGrams) {
            LOGGER.warn("Mixing exceeds maximum allowed dough size ({}g > {}g)",
                    totalGramsExact, maxDoughWeightGrams);
            return ItemStack.EMPTY;
        }

        clear();
        return dough;
    }

    public ListTag saveIngredientList() {
        ListTag list = new ListTag();
        for (IngredientStack st : ingredientList) {
            CompoundTag t = new CompoundTag();
            t.putString("Item", BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString());
            t.putString("Category", st.getCategory().name());
            t.putInt("Milligrams", st.getMilligrams());
            t.putInt("Grams", (int)Math.round(st.getMilligrams() / 1000.0));

            var ft = st.getFlourType();
            if (ft != null) t.putString("FlourType", ft.getId());
            list.add(t);
        }
        return list;
    }

    public void loadIngredientList(ListTag list) {
        ingredientList.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            var id = ResourceLocation.tryParse(t.getString("Item"));
            var item = BuiltInRegistries.ITEM.get(id);
            IngredientCategory cat = IngredientCategory.valueOf(t.getString("Category"));

            int mg;
            if (t.contains("Milligrams")) {
                mg = t.getInt("Milligrams");
            } else {
                mg = Math.max(0, t.getInt("Grams") * 1000);
            }

            FlourType ft = null;
            if (cat == IngredientCategory.FLOUR && t.contains("FlourType")) {
                ft = net.boulangermod.boulanger.item.FlourItemType.fromId(t.getString("FlourType")).toFlourType();
            }

            ingredientList.add(new IngredientStack(item, cat, ft, mg));
        }
    }

    public CompoundTag savePreciseTotals() {
        CompoundTag totals = new CompoundTag();
        for (var e : preciseTotalsG.entrySet()) {
            totals.putDouble(e.getKey().name(), e.getValue());
        }
        return totals;
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