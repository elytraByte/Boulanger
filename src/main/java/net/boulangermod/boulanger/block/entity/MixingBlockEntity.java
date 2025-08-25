package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.*;
import net.boulangermod.boulanger.screen.MixingBlockMenu;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class MixingBlockEntity extends AbstractProcessingBlockEntity
        implements AbstractProcessingBlock.Tickable {

    private static final Logger LOGGER = LogManager.getLogger();

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
        if (level.isClientSide) return;

        // Handle weighed‐ingredient bowl input
        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && isWeighedIngredient(in)) {
            var grams = in.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).grams();
            var cat   = in.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
            LOGGER.debug("Input bowl: {} of category {}", fmtWeightG(grams), cat);


            addIngredientFromBowl(in);

            // best-effort log of latest totals for that category
            double catTotal = preciseTotalsG.getOrDefault(cat, 0.0);
            LOGGER.info("Added ingredient {} → category total now {}", cat, fmtWeightG(catTotal));


            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);
            spawnEmptyBowl();
        }

        // Mixing progress
        if (mixing) {
            mixProgress++;
            if (mixProgress >= MAX_MIX_TIME) {
                generateDough();
                mixing = false;
                mixProgress = 0;
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
        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializers.RATIO_TYPE.get());

        double totalFlour = calculateTotalFlour();
        if (totalFlour <= 0) return Optional.empty();

        for (var holder : recipes) {
            RatioRecipe recipe = holder.value();
            boolean matches = matchesIngredientComponents(recipe)
                    && hasEnoughTotalWeight(recipe)
                    && meetsItemRequirements(recipe);

            if (matches) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private boolean matchesIngredientComponents(RatioRecipe recipe) {
        // 1) Sum up everything (exact grams from mg)
        double totalWeight = ingredientList.stream()
                .mapToDouble(st -> st.getMilligrams() / 1000.0)
                .sum();

        // Not even close to one batch?
        if (totalWeight < recipe.getServingWeight() * (1 - recipe.getTolerance())) {
            return false;
        }

        // 2) Compute the *single-serving* flour weight from serving weight & baker's %
        double nonFlourPctSum = recipe.getComponents().stream()
                .filter(c -> c.category() != IngredientCategory.FLOUR)
                .mapToDouble(IngredientComponent::targetPercent)
                .sum();

        double singleFlourWeight = recipe.getServingWeight()
                / (1.0 + nonFlourPctSum / 100.0);

        // 3) “1 % of flour” in grams for one serving
        double gramsPerPct = singleFlourWeight / 100.0;

        // 4) How many whole batches do we actually have?
        double rawBatches = totalWeight / recipe.getServingWeight();
        int batchCount = (int) Math.floor(rawBatches + recipe.getTolerance());
        if (batchCount < 1) return false;

        // 5) For each component, check actual vs expected = singleTarget * batchCount
        for (IngredientComponent comp : recipe.getComponents()) {
            // one-serving target
            double singleTarget = gramsPerPct * comp.targetPercent();
            // scaled for N servings
            double expected = singleTarget * batchCount;
            // relative tolerance (scaled)
            double relTolG = singleTarget * recipe.getTolerance() * batchCount;

            // actual grams from precise mg, filtered by category AND allowed items(if any)
            double actual = ingredientList.stream()
                    .filter(st -> st.getCategory() == comp.category())
                    .filter(st -> comp.allowedItems().isEmpty()
                            || comp.allowedItems().contains(getMatchId(st, comp.category())))
                    .mapToDouble(st -> st.getMilligrams() / 1000.0)
                    .sum();

            if (!withinTolerance(actual, expected, relTolG, batchCount)) {
                LOGGER.debug("   ✗ {}: got {}g vs expected {}g (±{}g rel, ±{}g abs)",
                        comp.category(),
                        String.format("%.3f", actual),
                        String.format("%.3f", expected),
                        String.format("%.3f", relTolG),
                        String.format("%.3f", ABS_EPS_G_PER_BATCH * batchCount));
                return false;
            }
        }

        // 6) Finally, ensure totalWeight ≈ batchCount × servingWeight
        double totalTol = recipe.getServingWeight() * recipe.getTolerance() * batchCount;
        if (!withinTolerance(totalWeight, recipe.getServingWeight() * batchCount, totalTol, batchCount)) {
            LOGGER.debug("   ✗ Total dough {}g vs {}×{}g (±{}g rel, ±{}g abs)",
                    String.format("%.3f", totalWeight),
                    batchCount,
                    recipe.getServingWeight(),
                    String.format("%.3f", totalTol),
                    String.format("%.3f", ABS_EPS_G_PER_BATCH * batchCount));
            return false;
        }

        return true;
    }

    private static boolean withinTolerance(double actual, double expected, double relTolG, int batchCount) {
        double diff = Math.abs(actual - expected);
        double absTol = ABS_EPS_G_PER_BATCH * batchCount;
        return diff <= relTolG || diff <= absTol;
    }

    private double calculateTotalFlour() {
        // grams as double, from mg, precise
        return ingredientList.stream()
                .filter(st -> st.getCategory() == IngredientCategory.FLOUR)
                .mapToDouble(st -> st.getMilligrams() / 1000.0)
                .sum();
    }

    private boolean hasEnoughTotalWeight(RatioRecipe recipe) {
        double totalGrams = ingredientList.stream()
                .mapToDouble(st -> st.getMilligrams() / 1000.0)
                .sum();

        double minNeeded = recipe.getServingWeight() * (1.0 - recipe.getTolerance()); // lower bound only
        boolean ok = totalGrams + 1e-9 >= minNeeded; // tiny epsilon

        if (!ok) {
            LOGGER.debug("   ✗ Total grams {}g is below minimum {}g for one batch (serving {}g, tol ±{}%)",
                    String.format("%.3f", totalGrams),
                    String.format("%.3f", minNeeded),
                    recipe.getServingWeight(),
                    recipe.getTolerance() * 100.0);
        }
        return ok;
    }

    private boolean meetsItemRequirements(RatioRecipe recipe) {
        for (var req : recipe.getItemRequirements()) {
            double got = ingredientList.stream()
                    .filter(st -> resolveIngredientId(st).equals(req.getItemId()))
                    .mapToDouble(st -> st.getMilligrams() / 1000.0)
                    .sum();
            if (got + 1e-9 < req.getAmount()) {
                LOGGER.debug("   ✗ Item requirement {}: need {}g, got {}g",
                        req.getItemId(), req.getAmount(), String.format("%.3f", got));
                return false;
            }
        }
        return true;
    }

    private ResourceLocation getMatchId(IngredientStack st, IngredientCategory category) {
        if (category == IngredientCategory.FLOUR) {
            // Use FlourType#getId(), not an enum name, and namespace it
            var ft = st.getFlourType();
            return (ft != null)
                    ? ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, ft.getId())
                    : BuiltInRegistries.ITEM.getKey(st.getActualItem());
        } else {
            return resolveIngredientId(st);
        }
    }

    private ResourceLocation resolveIngredientId(IngredientStack st) {
        if (st.getCategory() == IngredientCategory.FLOUR) {
            var ft = st.getFlourType();
            if (ft != null) {
                return ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, ft.getId());
            }
            LOGGER.debug("⚠ Missing FlourType for flour ingredient stack: {}", st);
            // fallback to invalid
            return ResourceLocation.fromNamespaceAndPath("minecraft", "air");
        }

        var itc = st.getBowlStack().get(ModDataComponentTypes.INGREDIENT_TYPE.get());
        if (itc != null) {
            return BuiltInRegistries.ITEM.getKey(itc.item());
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
        // Resolve category
        IngredientCategory cat = bowl.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                ? bowl.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                : IngredientCategory.getIngredientCategory(bowl);

        // Weight (grams -> mg)
        WeightComponent wc = bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (wc == null) return;
        int mg = Math.max(0, Math.round(wc.grams() * 1000f));
        if (mg <= 0) return;
        double g = mg / 1000.0;

        if (cat == IngredientCategory.FLOUR) {
            // FLOUR → keep FlourType, but set the IngredientStack's actual item to a flour item
            FlourType ft = bowl.get(ModDataComponentTypes.FLOUR_TYPE.get());

            // Prefer a concrete registry item "boulanger:<flour_id>" if you have them…
            Item flourItem = Items.AIR;
            if (ft != null) {
                var id = ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, ft.getId());
                flourItem = BuiltInRegistries.ITEM.get(id);
            }
            // …otherwise fall back to your single base flour item with model overrides
            if (flourItem == Items.AIR) {
                flourItem = ModItems.FLOUR_ITEM.get(); // <-- change if your base flour item has a different name
            }

            ingredientList.add(new IngredientStack(flourItem, IngredientCategory.FLOUR, /*flourType*/ ft, mg));
            preciseTotalsG.merge(cat, g, Double::sum);
            syncToClient();
            return;
        }

        // NON-FLOUR → resolve the exact ingredient item id from components
        ResourceLocation bowlIngId = ingredientIdFromBowl(bowl, cat);
        Item ingItem = (bowlIngId != null) ? BuiltInRegistries.ITEM.get(bowlIngId) : Items.AIR;

        if (ingItem == Items.AIR) {
            // last resort, try whatever you used to carry in old worlds
            ingItem = bowl.getItem(); // may still be BOWL if components are missing
        }

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

        // 3) Baker percentages (category → summed target %)
        Map<IngredientCategory, Double> targetMap = ratio.getComponents().stream()
                .collect(Collectors.groupingBy(
                        IngredientComponent::category,
                        Collectors.summingDouble(IngredientComponent::targetPercent)
                ));
        dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), new BakerPctComponent(targetMap));

        // 4) DoughRecipeComponent + weight (store per-ingredient rounded grams; keep precision internally)
        List<IngredientInfo> infos = new ArrayList<>();
        int totalMg = 0;

        for (IngredientStack st : ingredientList) {
            String itemId = Optional.ofNullable(
                            st.getBowlStack().get(ModDataComponentTypes.FLOUR_TYPE.get()))
                    .map(FlourType::getId)
                    .orElse(BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString());

            int mg = st.getMilligrams(); // exact mg from the stack
            int gramsRounded = (int) Math.round(mg / 1000.0);

            totalMg += mg;
            infos.add(new IngredientInfo(itemId, st.getCategory(), gramsRounded));
        }

        int totalGramsRounded = (int) Math.round(totalMg / 1000.0);

        dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                new DoughRecipeComponent(ratio.getId(), targetMap, infos, totalGramsRounded));
        dough.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                new WeightComponent((float) totalGramsRounded));

        // 5) Proofing state & link to process recipe
        dough.set(ModDataComponentTypes.PROOFING_STATE.get(),
                new ProofingStateComponent(0, /*ticks=*/0, /*shaped=*/false));
        dough.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), ratio.getId());

        // 6) Tag the recipe’s pan on the dough (PanTypeComponent)
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

        // 7) Final size check and output
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
