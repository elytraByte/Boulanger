package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.FlourItemType;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

    private static final double MAX_DOUGH_WEIGHT_GRAMS = 22680.0; // 20 kg default for basic mixer

    private final List<IngredientStack> ingredientList = new ArrayList<>();
    private boolean mixing = false;
    private int mixProgress = 0;

    private static final int MAX_MIX_TIME = 100;

    public MixingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXING_BLOCK_BE.get(), pos, state, 3);
    }

    public List<IngredientStack> getIngredientList() { return Collections.unmodifiableList(ingredientList); }
    public int getMixProgress() { return mixProgress; }
    public boolean isMixing() { return mixing; }
    public static int getMaxMixTime() { return MAX_MIX_TIME; }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Handle weighed‐ingredient bowl input
        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && isWeighedIngredient(in)) {
            LOGGER.debug("Input bowl: {}g of category {}",
                    in.get(ModDataComponentTypes.INGREDIENT_GRAMS.get()).grams(),
                    in.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get()));

            addIngredientFromBowl(in);
            LOGGER.info("Added ingredient {} → {}g",
                    ingredientList.get(ingredientList.size()-1).getCategory(),
                    ingredientList.get(ingredientList.size()-1).getGrams());

            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);
            spawnEmptyBowl();
        }

        // Mixing progress
        if (mixing) {
            mixProgress++;
            LOGGER.debug("Mixing progress: {}/{}", mixProgress, MAX_MIX_TIME);
            if (mixProgress >= MAX_MIX_TIME) {
                generateDough();
                mixing = false;
                mixProgress = 0;
            }
        }
    }

    private void spawnEmptyBowl() {
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_BOWL);
        if (out.isEmpty() || out.getItem() != Items.BOWL) {
            itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL));
        } else {
            out.grow(1);
            itemHandler.setStackInSlot(OUTPUT_BOWL, out);
        }
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

    private double calculateTotalFlour() {
        return ingredientList.stream()
                .filter(st -> st.getCategory() == IngredientCategory.FLOUR)
                .mapToDouble(IngredientStack::getGrams)
                .sum();
    }

    private boolean matchesIngredientComponents(RatioRecipe recipe) {
        // 1) Sum up everything
        double totalWeight = ingredientList.stream()
                .mapToDouble(IngredientStack::getGrams)
                .sum();
        if (totalWeight < recipe.getServingWeight() * (1 - recipe.getTolerance())) {
            // not even close to one batch
            return false;
        }

        // 2) Compute the *single-serving* flour weight
        double nonFlourPctSum = recipe.getComponents().stream()
                .filter(c -> c.category() != IngredientCategory.FLOUR)
                .mapToDouble(IngredientComponent::targetPercent)
                .sum();
        double singleFlourWeight = recipe.getServingWeight()
                / (1.0 + nonFlourPctSum / 100.0);

        // 3) “1 % of flour” in grams for one serving
        double gramsPerPct = singleFlourWeight / 100.0;

        // 4) How many whole batches do we actually have?
        //    We allow a little slack so that slight rounding drift still rounds to the intended batchCount.
        double rawBatches = totalWeight / recipe.getServingWeight();
        int batchCount = (int) Math.floor(rawBatches + recipe.getTolerance());
        if (batchCount < 1) return false;

        // 5) For each component, check actual vs expected = singleTarget * batchCount
        for (IngredientComponent comp : recipe.getComponents()) {
            // one-serving target
            double singleTarget = gramsPerPct * comp.targetPercent();
            // scaled for N servings
            double expected = singleTarget * batchCount;
            // tolerance also scales
            double tol = singleTarget * recipe.getTolerance() * batchCount;

            double actual = ingredientList.stream()
                    .filter(st -> st.getCategory() == comp.category())
                    .filter(st -> comp.allowedItems().isEmpty()
                            || comp.allowedItems().contains(getMatchId(st, comp.category())))
                    .mapToDouble(IngredientStack::getGrams)
                    .sum();

            if (Math.abs(actual - expected) > tol) {
                LOGGER.debug("   ✗ {}: got {}g vs expected {}g (±{}g)",
                        comp.category(), actual, expected, tol);
                return false;
            }
        }

        // 6) Finally, ensure totalWeight ≈ batchCount × servingWeight
        double totalTol = recipe.getServingWeight() * recipe.getTolerance() * batchCount;
        if (Math.abs(totalWeight - recipe.getServingWeight() * batchCount) > totalTol) {
            LOGGER.debug("   ✗ Total dough {}g vs {}×{}g (±{}g)",
                    totalWeight, batchCount, recipe.getServingWeight(), totalTol);
            return false;
        }

        return true;
    }

    private boolean hasEnoughTotalWeight(RatioRecipe recipe) {
        double totalWeight = ingredientList.stream()
                .mapToDouble(IngredientStack::getGrams)
                .sum();
        return totalWeight >= recipe.getServingWeight();
    }

    private boolean meetsItemRequirements(RatioRecipe recipe) {
        for (var req : recipe.getItemRequirements()) {
            double got = ingredientList.stream()
                    .filter(st -> resolveIngredientId(st).equals(req.getItemId()))
                    .mapToDouble(IngredientStack::getGrams)
                    .sum();
            if (got < req.getAmount()) return false;
        }
        return true;
    }

    private boolean isAllowedItem(IngredientStack st, IngredientComponent comp) {
        ResourceLocation matchId = getMatchId(st, comp.category());
        boolean allowed = comp.allowedItems().isEmpty() || comp.allowedItems().contains(matchId);
        LOGGER.debug("   → Matching {} for {}: allowed = {}", matchId, comp.category(), allowed);
        return allowed;
    }

    private ResourceLocation getMatchId(IngredientStack st, IngredientCategory category) {
        if (category == IngredientCategory.FLOUR) {
            var flourType = st.getFlourType();
            return flourType != null
                    ? ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, flourType.type())
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
            return ResourceLocation.fromNamespaceAndPath("minecraft", "air");
            // fallback to invalid
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
                LOGGER.info(" - {} : {}g", st.getCategory(), st.getGrams())
        );
        if (ingredientList.isEmpty()) {
            LOGGER.warn("No ingredients! Nothing to mix.");
            return;
        }
        mixing = true;
        mixProgress = 0;
    }

    private void generateDough() {
        LOGGER.debug("Generating dough at {}", worldPosition);

        // 1) Find matching ratio recipe
        Optional<RatioRecipe> optRatio = findMatchingRecipe();
        if (optRatio.isEmpty()) {
            LOGGER.info("No valid recipe — clearing output.");
            itemHandler.setStackInSlot(OUTPUT_DOUGH, ItemStack.EMPTY);
            ingredientList.clear();
            return;
        }
        RatioRecipe ratio = optRatio.get();
        LOGGER.info("Creating dough for {}", ratio.getId());

        // 2) Build the raw dough stack
        ItemStack dough = new ItemStack(ModItems.DOUGH.get());

        // 3) Baker percentages
        Map<IngredientCategory, Double> targetMap = ratio.getComponents().stream()
                .collect(Collectors.groupingBy(
                        IngredientComponent::category,
                        Collectors.summingDouble(IngredientComponent::targetPercent)
                ));
        dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                new BakerPctComponent(targetMap));

        // 4) DoughRecipeComponent + weight
        List<IngredientInfo> infos = new ArrayList<>();
        int totalGrams = 0;
        for (IngredientStack st : ingredientList) {
            String itemId = Optional.ofNullable(
                            st.getBowlStack().get(ModDataComponentTypes.FLOUR_TYPE.get())
                    )
                    .map(FlourType::getId)
                    .orElse(BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString());

            int grams = st.getGrams();
            totalGrams += grams;
            infos.add(new IngredientInfo(itemId, st.getCategory(), grams));
        }
        dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                new DoughRecipeComponent(ratio.getId(), targetMap, infos, totalGrams));
        dough.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                new WeightComponent(totalGrams));

        // 5) Proofing state & link to process recipe
        dough.set(ModDataComponentTypes.PROOFING_STATE.get(),
                new ProofingStateComponent(0, /*ticks=*/0, /*shaped=*/false));
        dough.set(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(), ratio.getId());

        // 6) **Tag on the recipe’s pan — using a PanTypeComponent wrapper**
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
                    // <-- Here’s the key change:
                    dough.set(
                            ModDataComponentTypes.PAN_TYPE.get(),
                            new PanTypeComponent(panEnum.getId())
                    );
                } else {
                    LOGGER.warn("Unknown pan type '{}' for recipe {}", panLoc, ratio.getId());
                }
            }
        } else {
            LOGGER.warn("⚠ No DoughProcessRecipe found for dough type: {}", ratio.getId());
        }

        // 7) Final size check and output
        if (totalGrams > MAX_DOUGH_WEIGHT_GRAMS) {
            LOGGER.warn("Mixing exceeds maximum allowed dough size ({}g > {}g)",
                    totalGrams, MAX_DOUGH_WEIGHT_GRAMS);
            return;
        }
        itemHandler.setStackInSlot(OUTPUT_DOUGH, dough);
        ingredientList.clear();
    }

    private boolean isWeighedIngredient(ItemStack s) {
        return s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
    }

    public void addIngredientFromBowl(ItemStack bowl) {
        IngredientCategory cat = bowl.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                ? bowl.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                : IngredientCategory.getIngredientCategory(bowl);
        WeightComponent wc = bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (wc == null) return;
        int grams = (int) wc.grams();
        FlourType newFt = bowl.has(ModDataComponentTypes.FLOUR_TYPE.get())
                ? bowl.get(ModDataComponentTypes.FLOUR_TYPE.get())
                : null;

        for (IngredientStack st : ingredientList) {
            if (st.getCategory() != cat) continue;
            if (cat == IngredientCategory.FLOUR) {
                if (Objects.equals(st.getFlourType(), newFt)) {
                    st.addGrams(grams);
                    return;
                }
            } else {
                st.addGrams(grams);
                return;
            }
        }
        ingredientList.add(new IngredientStack(bowl.copy()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        //tag.put("Inventory", itemHandler.serializeNBT(regs));
        tag.putBoolean("Mixing", mixing);
        tag.putInt("MixProgress", mixProgress);
        tag.put("Ingredients", saveIngredientList());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        //itemHandler.deserializeNBT(regs, tag.getCompound("Inventory"));
        mixing = tag.getBoolean("Mixing");
        mixProgress = tag.getInt("MixProgress");
        loadIngredientList(tag.getList("Ingredients", ListTag.TAG_COMPOUND));
    }

    private ListTag saveIngredientList() {
        ListTag list = new ListTag();
        for (IngredientStack st : ingredientList) {
            CompoundTag t = new CompoundTag();
            t.putString("Item",
                    BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString());
            t.putString("Category", st.getCategory().name());
            t.putInt("Grams", st.getGrams());
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
            ResourceLocation id = ResourceLocation.tryParse(t.getString("Item"));
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            IngredientCategory cat = IngredientCategory.valueOf(t.getString("Category"));
            int grams = t.getInt("Grams");
            stack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                    new WeightComponent(grams));
            stack.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), cat);
            if (cat == IngredientCategory.FLOUR && t.contains("FlourType")) {
                var ft = FlourItemType.fromId(t.getString("FlourType")).toFlourType();
                stack.set(ModDataComponentTypes.FLOUR_TYPE.get(), ft);
            }
            ingredientList.add(new IngredientStack(stack));
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
}
