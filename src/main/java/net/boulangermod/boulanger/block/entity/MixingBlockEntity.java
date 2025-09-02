package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.*;
import net.boulangermod.boulanger.screen.MixingBlockMenu;
import net.boulangermod.boulanger.util.DoughRecipeCanonicalier;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.IngredientStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MixingBlockEntity extends AbstractProcessingBlockEntity
        implements AbstractProcessingBlock.Tickable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation GENERIC_FLOUR_ID =
            ResourceLocation.fromNamespaceAndPath("boulanger", "flour");

    public static final int INPUT_BOWL   = 0;
    public static final int OUTPUT_BOWL  = 1;
    public static final int OUTPUT_DOUGH = 2;

    // Allow very small absolute drift for micro-ingredients (per batch)
    private static final double ABS_EPS_G_PER_BATCH = 0.05; // 50 mg

    private static final double MAX_DOUGH_WEIGHT_GRAMS = 22680.0; // 20 kg default for basic mixer
    private static final int MAX_MIX_TIME = 100;

    // Category totals in GRAMS with full precision
    private final Map<IngredientCategory, Double> preciseTotalsG = new EnumMap<>(IngredientCategory.class);

    private final List<IngredientStack> ingredientList = new ArrayList<>();
    private boolean mixing = false;
    private int mixProgress = 0;

    public MixingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXING_BLOCK_BE.get(), pos, state, 3);
    }

    public List<IngredientStack> getIngredientList() { return Collections.unmodifiableList(ingredientList); }
    public int getMixProgress() { return mixProgress; }
    public boolean isMixing() { return mixing; }
    public static int getMaxMixTime() { return MAX_MIX_TIME; }

    // pretty-print a weight when you have grams as a double
    private static String fmtWeightG(double grams) {
        int mg = (int) Math.round(grams * 1000.0);
        if (Math.abs(grams) < 1.0) {
            return String.format(Locale.ROOT, "%d mg (%.3f g)", mg, grams);
        } else {
            return String.format(Locale.ROOT, "%.3f g (%d mg)", grams, mg);
        }
    }

    // pretty-print a weight when you have milligrams as an int
    private static String fmtWeightMg(int mg) {
        double g = mg / 1000.0;
        if (Math.abs(g) < 1.0) {
            return String.format(Locale.ROOT, "%d mg (%.3f g)", mg, g);
        } else {
            return String.format(Locale.ROOT, "%.3f g (%d mg)", g, mg);
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide) return;

        // --- Intake: consume a filled bowl from INPUT_BOWL, record ingredient, return empty bowl ---
        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && isWeighedIngredient(in)) {
            var grams = in.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).grams();
            var cat   = in.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
            LOGGER.debug("Input bowl: {} g of category {}", grams, cat);

            addIngredientFromBowl(in);

            // clear input, spawn an empty bowl to OUTPUT_BOWL (or drop if full)
            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);
            spawnEmptyBowl();
            syncToClient();
        }

        // --- Mixing progress ---
        if (mixing) {
            mixProgress++;
            if (mixProgress >= MAX_MIX_TIME) {
                generateDough();
                mixing = false;
                mixProgress = 0;
                syncToClient();
            }
        }
    }

    private void spawnEmptyBowl() {
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_BOWL);
        final int limit = Math.min(64, itemHandler.getSlotLimit(OUTPUT_BOWL));

        if (out.isEmpty() || out.getItem() != Items.BOWL) {
            itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL));
        } else if (out.getCount() < limit) {
            out.grow(1);
            itemHandler.setStackInSlot(OUTPUT_BOWL, out);
        } else {
            if (level != null && !level.isClientSide) {
                net.minecraft.world.level.block.Block.popResource(level, worldPosition, new ItemStack(Items.BOWL));
            }
        }
        syncToClient();
    }

    private Optional<RatioRecipe> findMatchingRecipe() {
        // 1) Collect bowls by category and by canonical id (FlourType RL for FLOUR; item RL for others)
        Map<IngredientCategory, Integer> totalsMg = new EnumMap<>(IngredientCategory.class);
        Map<IngredientCategory, Map<ResourceLocation, Integer>> perIdMg = new EnumMap<>(IngredientCategory.class);

        for (IngredientStack st : ingredientList) {
            int mg = Math.max(0, st.getMilligrams());
            if (mg == 0) continue;

            IngredientCategory cat = st.getCategory();
            totalsMg.merge(cat, mg, Integer::sum);

            ResourceLocation rid = canonicalIdForMatching(st); // FLOUR → flourType RL; others → actual item RL
            perIdMg.computeIfAbsent(cat, k -> new HashMap<>()).merge(rid, mg, Integer::sum);
        }

        int flourMg = totalsMg.getOrDefault(IngredientCategory.FLOUR, 0);
        if (flourMg <= 0) return Optional.empty();

        // (Debug) flour ids gathered
        Map<ResourceLocation, Integer> flourMap = perIdMg.get(IngredientCategory.FLOUR);
        if (flourMap != null && !flourMap.isEmpty()) {
            String dbg = flourMap.entrySet().stream()
                    .map(e -> e.getKey() + "=" + (e.getValue() / 1000.0) + "g")
                    .toList()
                    .toString();
            LOGGER.debug("FLOUR perId (matcher): {}", dbg);
        }

        // 2) Try ratio recipes
        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializers.RATIO_TYPE.get());
        for (var holder : recipes) {
            RatioRecipe recipe = holder.value();
            double tolPctPoints = recipe.getTolerance() * 100.0; // e.g., 0.05 -> ±5.0 pp

            LOGGER.debug("Checking recipe {}", recipe.getId());

            boolean ok = true;
            for (IngredientComponent comp : recipe.getComponents()) {
                IngredientCategory cat = comp.category();
                double targetPct = comp.targetPercent(); // baker's %

                // Got (mg): whole category if no whitelist; otherwise sum only allowed ids
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

                double gotPct = flourMg == 0 ? 0.0 : (gotMgInt * 100.0) / flourMg; // baker's %
                double deltaPct = Math.abs(gotPct - targetPct);

                if (deltaPct > tolPctPoints + 1e-9) {
                    // Debug: show both % and grams context (pre-format with String.format)
                    double expectedMg = flourMg * (targetPct / 100.0);
                    String msg = String.format(Locale.ROOT,
                            "   ✗ %s: got %.3fg (%.3f%%), expected %.3f%% (%.3fg), tol ±%.3f pp",
                            cat, gotMgInt / 1000.0, gotPct, targetPct, Math.round(expectedMg) / 1000.0, tolPctPoints);
                    LOGGER.debug(msg);

                    if (allowedIds != null && !allowedIds.isEmpty()) {
                        Map<ResourceLocation, Integer> byId = perIdMg.getOrDefault(cat, Map.of());
                        String haveDbg = byId.entrySet().stream()
                                .map(e -> e.getKey() + "=" + (e.getValue() / 1000.0) + "g")
                                .toList()
                                .toString();
                        String allowDbg = allowedIds.stream().map(Object::toString).toList().toString();
                        LOGGER.debug("      allowed IDs: {}", allowDbg);
                        LOGGER.debug("      have by-id : {}", haveDbg);
                    }

                    ok = false;
                    break;
                }
            }

            if (ok) {
                LOGGER.debug("   ✓ matched {}", recipe.getId());
                return Optional.of(recipe);
            }
        }

        return Optional.empty();
    }

    /** FLOUR → use FlourType id (e.g. boulanger:all_purpose_flour); others → actual item id. */
    private static ResourceLocation canonicalIdForMatching(IngredientStack st) {
        if (st.getCategory() == IngredientCategory.FLOUR) {
            FlourType ft = st.getFlourType();
            if (ft != null) {
                String id = ft.getId(); // "all_purpose_flour" or "boulanger:all_purpose_flour"
                return (id.indexOf(':') >= 0)
                        ? ResourceLocation.parse(id)
                        : ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, id);
            }
            return BuiltInRegistries.ITEM.getKey(st.getActualItem()); // rare fallback
        }
        return BuiltInRegistries.ITEM.getKey(st.getActualItem());
    }

    public void startMixing() {
        LOGGER.info("Start mixing. Ingredients:");
        ingredientList.forEach(st ->
                LOGGER.info(" - {} : {}", st.getCategory(), fmtWeightMg(st.getMilligrams())));
        if (ingredientList.isEmpty()) {
            LOGGER.warn("No ingredients! Nothing to mix.");
            return;
        }
        mixing = true;
        mixProgress = 0;
    }

    private ResourceLocation ingredientIdFromBowl(ItemStack bowl, IngredientCategory cat) {
        if (cat == IngredientCategory.FLOUR) {
            FlourType ft = bowl.get(ModDataComponentTypes.FLOUR_TYPE.get());
            if (ft != null) {
                return ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, ft.getId());
            }
            return null;
        }

        // Non-flour: prefer INGREDIENT_TYPE component
        var itc = bowl.get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (itc != null) {
            return BuiltInRegistries.ITEM.getKey(itc.item());
        }
        return null; // unknown
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
            // Always use your single flour item; distinguish by FlourType component
            Item flourItem = ModItems.FLOUR_ITEM.get();
            ingredientList.add(new IngredientStack(flourItem, IngredientCategory.FLOUR, ft, mg));
            preciseTotalsG.merge(cat, g, Double::sum);
            syncToClient();
            return;
        }

        // Non-flour: resolve an exact item id from components (if present), else fall back to the item.
        ResourceLocation bowlIngId = ingredientIdFromBowl(bowl, cat);
        Item ingItem = (bowlIngId != null) ? BuiltInRegistries.ITEM.get(bowlIngId) : bowl.getItem();

        ingredientList.add(new IngredientStack(ingItem, cat, /*flourType*/ null, mg));
        preciseTotalsG.merge(cat, g, Double::sum);
        syncToClient();
    }

    private void generateDough() {
        LOGGER.debug("Generating dough at {}", worldPosition);

        // 1) Find matching ratio recipe
        Optional<RatioRecipe> optRatio = findMatchingRecipe();
        if (optRatio.isEmpty()) {
            LOGGER.info("No valid recipe — clearing output.");
            itemHandler.setStackInSlot(OUTPUT_DOUGH, ItemStack.EMPTY);
            ingredientList.clear();
            preciseTotalsG.clear();
            return;
        }
        RatioRecipe ratio = optRatio.get();
        LOGGER.info("Creating dough for {}", ratio.getId());

        // 2) Build the raw dough stack
        ItemStack dough = new ItemStack(ModItems.DOUGH.get());

        // 3) Baker percentages (category → summed target %), keep exactly as-is (no rounding)
        Map<IngredientCategory, Double> targetMap = ratio.getComponents().stream()
                .collect(Collectors.groupingBy(
                        IngredientComponent::category,
                        Collectors.summingDouble(IngredientComponent::targetPercent)
                ));
        dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), new BakerPctComponent(targetMap));

        // 4) Canonicalize ingredients (merge + deterministic sort).
        //    Build inputs and remember FlourType per (category|itemIdRL)
        List<DoughRecipeCanonicalier.Ingredient> canonInputs = new ArrayList<>();
        Map<String, FlourType> flourTypeByKey = new HashMap<>();
        // (debug) track flour per-id so we can see the canonical flour RLs & grams
        Map<ResourceLocation, Integer> debugFlourPerIdMg = new HashMap<>();

        int totalMgUncanonical = 0;

        for (IngredientStack st : ingredientList) {
            final IngredientCategory catEnum = st.getCategory();
            final String cat = catEnum.name();
            final int mg = Math.max(0, st.getMilligrams());

            // —— Normalize to canonical item id (RL).
            ResourceLocation itemId;
            if (catEnum == IngredientCategory.FLOUR) {
                FlourType ft = st.getFlourType();
                if (ft != null) {
                    // Accept either "boulanger:all_purpose_flour" or "all_purpose_flour"
                    String idStr = ft.getId();
                    itemId = (idStr.indexOf(':') >= 0)
                            ? ResourceLocation.parse(idStr)
                            : ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, idStr);
                } else {
                    // Should be rare; fall back to the base flour item id
                    itemId = BuiltInRegistries.ITEM.getKey(st.getActualItem());
                    LOGGER.warn("Flour stack missing FlourType; falling back to {}", itemId);
                }
            } else {
                // Non-flour: use the actual item id we stored on the IngredientStack
                itemId = BuiltInRegistries.ITEM.getKey(st.getActualItem());
            }

            LOGGER.debug("canon: {} {}mg -> {}", cat, mg, itemId);

            // collect canonical inputs for the dough's saved recipe component
            canonInputs.add(new DoughRecipeCanonicalier.Ingredient(cat, itemId, mg));
            totalMgUncanonical += mg;

            if (catEnum == IngredientCategory.FLOUR) {
                FlourType ft = st.getFlourType();
                if (ft != null) {
                    flourTypeByKey.put(cat + "|" + itemId.toString(), ft);
                }
                // debug aggregation
                debugFlourPerIdMg.merge(itemId, mg, Integer::sum);
            }
        }

        // Debug: confirm which flour ids & amounts we canon’d
        if (!debugFlourPerIdMg.isEmpty()) {
            String dbg = debugFlourPerIdMg.entrySet().stream()
                    .map(e -> e.getKey() + "=" + (e.getValue() / 1000.0) + "g")
                    .collect(Collectors.joining(", "));
            LOGGER.debug("FLOUR perId (canonical): {}", dbg);
        }

        // Minimal keys/vals from targetMap (record shape correctness only)
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

        // Map back to IngredientInfo, re-attaching FlourType when present
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

        // 5) Write components
        dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                new DoughRecipeComponent(ratio.getId(), targetMap, infos, totalGramsRounded));
        dough.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                new WeightComponent((float) totalGramsRounded));

        // 6) Proofing state & link to process recipe
        dough.set(ModDataComponentTypes.PROOFING_STATE.get(),
                new ProofingStateComponent(0, /*ticks=*/0, /*shaped=*/false));
        dough.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), ratio.getId());

        // 7) Tag the recipe’s pan on the dough (PanTypeComponent)
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

        // 8) Final size check and output
        double totalGramsExact = totalMg / 1000.0;
        if (totalGramsExact > MAX_DOUGH_WEIGHT_GRAMS) {
            LOGGER.warn("Mixing exceeds maximum allowed dough size ({}g > {}g)",
                    totalGramsExact, MAX_DOUGH_WEIGHT_GRAMS);
            return;
        }

        itemHandler.setStackInSlot(OUTPUT_DOUGH, dough);
        ingredientList.clear();
        preciseTotalsG.clear();
    }

    private boolean isWeighedIngredient(ItemStack s) {
        return s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("Mixing", mixing);
        tag.putInt("MixProgress", mixProgress);
        tag.put("Ingredients", saveIngredientList());

        // Persist preciseTotalsG for safety (optional; can be rebuilt)
        CompoundTag totals = new CompoundTag();
        for (var e : preciseTotalsG.entrySet()) {
            totals.putDouble(e.getKey().name(), e.getValue());
        }
        tag.put("PreciseTotalsG", totals);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        mixing = tag.getBoolean("Mixing");
        mixProgress = tag.getInt("MixProgress");
        loadIngredientList(tag.getList("Ingredients", ListTag.TAG_COMPOUND));

        // Rebuild or read precise totals
        preciseTotalsG.clear();
        if (tag.contains("PreciseTotalsG")) {
            CompoundTag totals = tag.getCompound("PreciseTotalsG");
            for (IngredientCategory c : IngredientCategory.values()) {
                if (totals.contains(c.name())) {
                    preciseTotalsG.put(c, totals.getDouble(c.name()));
                }
            }
        } else {
            // Recompute from ingredientList
            for (IngredientStack st : ingredientList) {
                preciseTotalsG.merge(st.getCategory(), st.getMilligrams() / 1000.0, Double::sum);
            }
        }
    }

    private ListTag saveIngredientList() {
        ListTag list = new ListTag();
        for (IngredientStack st : ingredientList) {
            CompoundTag t = new CompoundTag();
            t.putString("Item", BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString());
            t.putString("Category", st.getCategory().name());
            t.putInt("Milligrams", st.getMilligrams()); // precise
            // optional: also write legacy grams for older worlds/tools
            t.putInt("Grams", (int)Math.round(st.getMilligrams() / 1000.0));

            var ft = st.getFlourType();
            if (ft != null) t.putString("FlourType", ft.getId());
            list.add(t);
        }
        return list;
    }

    private void loadIngredientList(ListTag list) {
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
                // backward compat for worlds saved before this change
                mg = Math.max(0, t.getInt("Grams") * 1000);
            }

            FlourType ft = null;
            if (cat == IngredientCategory.FLOUR && t.contains("FlourType")) {
                ft = net.boulangermod.boulanger.item.FlourItemType.fromId(t.getString("FlourType")).toFlourType();
            }

            ingredientList.add(new IngredientStack(item, cat, ft, mg));
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable("mixing_block.boulanger");
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new MixingBlockMenu(id, inv, this);
    }

    @Override public void drops() {
        super.drops();
    }

    // ── Add inside class ───────────────────────────────────────────────────────────

    /** Mark dirty and push an update packet so the client sees ingredient changes. */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // NeoForge/1.21 style: send full tag to client
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider regs) {
        handleUpdateTag(pkt.getTag(), regs);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        tag.putBoolean("Mixing", mixing);
        tag.putInt("MixProgress", mixProgress);
        tag.put("Ingredients", saveIngredientList()); // send the list
        // Optional: also include precise totals for exact client mirrors
        CompoundTag totals = new CompoundTag();
        for (var e : preciseTotalsG.entrySet()) {
            totals.putDouble(e.getKey().name(), e.getValue());
        }
        tag.put("PreciseTotalsG", totals);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider regs) {
        super.handleUpdateTag(tag, regs);
        mixing = tag.getBoolean("Mixing");
        mixProgress = tag.getInt("MixProgress");
        if (tag.contains("Ingredients")) {
            loadIngredientList(tag.getList("Ingredients", ListTag.TAG_COMPOUND));
        }
        // Rebuild preciseTotalsG if present
        preciseTotalsG.clear();
        if (tag.contains("PreciseTotalsG")) {
            CompoundTag totals = tag.getCompound("PreciseTotalsG");
            for (IngredientCategory c : IngredientCategory.values()) {
                if (totals.contains(c.name())) {
                    preciseTotalsG.put(c, totals.getDouble(c.name()));
                }
            }
        } else {
            for (IngredientStack st : ingredientList) {
                preciseTotalsG.merge(st.getCategory(), st.getMilligrams() / 1000.0, Double::sum);
            }
        }
    }

}
