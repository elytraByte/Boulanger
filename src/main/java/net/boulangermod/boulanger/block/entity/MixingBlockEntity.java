package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.MixingContainer;
import net.boulangermod.boulanger.recipe.RatioRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Mixer block entity: collects weighed‐ingredient bowls,
 * builds a list of IngredientStacks, and when started,
 * compares against all registered RatioRecipe.TYPE recipes.
 */
public class MixingBlockEntity extends BlockEntity
        implements AbstractProcessingBlock.Tickable, MenuProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int INPUT_BOWL   = 0;
    public static final int OUTPUT_BOWL  = 1;
    public static final int OUTPUT_DOUGH = 2;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    private final List<IngredientStack> ingredientList = new ArrayList<>();
    private boolean mixing = false;
    private int mixProgress = 0;

    private static final int MAX_MIX_TIME     = 100;
    private static final double TOLERANCE_PCT = 2.0;

    public MixingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXING_BLOCK_BE.get(), pos, state);
        // removed ModRatioRecipes.registerDefaults()
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }
    public List<IngredientStack> getIngredientList() { return Collections.unmodifiableList(ingredientList); }
    public int getMixProgress() { return mixProgress; }
    public boolean isMixing()     { return mixing; }
    public static int getMaxMixTime()  { return MAX_MIX_TIME; }

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

    private Map<IngredientCategory, Double> calculateBakersPercentages(List<IngredientStack> ingredients) {
        // Sum weights per category
        Map<IngredientCategory, Integer> weightPerCat = new EnumMap<>(IngredientCategory.class);
        int flourTotal = 0;
        for (IngredientStack st : ingredients) {
            int g = st.getGrams();
            weightPerCat.merge(st.getCategory(), g, Integer::sum);
            if (st.getCategory() == IngredientCategory.FLOUR) {
                flourTotal += g;
            }
        }

        // Avoid divide-by-zero
        if (flourTotal <= 0) {
            return Collections.emptyMap();
        }

        // Compute percentages
        Map<IngredientCategory, Double> pct = new EnumMap<>(IngredientCategory.class);
        for (Map.Entry<IngredientCategory, Integer> entry : weightPerCat.entrySet()) {
            double percent = entry.getValue() / (double) flourTotal * 100.0;
            pct.put(entry.getKey(), percent);
        }
        return pct;
    }

    private Optional<RatioRecipe> findMatchingRecipe() {
        var recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.RATIO_TYPE.get());
        LOGGER.info("Found {} ratio recipes", recipes.size());

        // 1) compute baker’s % once:
        Map<IngredientCategory, Double> actualPct = calculateBakersPercentages(ingredientList);
        LOGGER.debug("Baker's percentages: {}", actualPct);

        // 2) test each recipe
        for (var holder : recipes) {
            RatioRecipe recipe = holder.value();
            LOGGER.info("Testing recipe: {}", recipe.getId());

            // a) whitelist check
            if (!recipe.checkAllowedItems(ingredientList)) {
                LOGGER.info(" → {} failed item‐whitelist", recipe.getId());
                continue;
            }

            // b) percentage match
            if (recipe.matches(actualPct)) {
                return Optional.of(recipe);
            }
        }

        LOGGER.warn("No recipe matched for current mixer contents");
        return Optional.empty();
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
        Optional<RatioRecipe> opt = findMatchingRecipe();
        if (opt.isEmpty()) {
            LOGGER.info("No valid recipe — clearing output.");
            itemHandler.setStackInSlot(OUTPUT_DOUGH, ItemStack.EMPTY);
            ingredientList.clear();
            return;
        }
        RatioRecipe recipe = opt.get();
        LOGGER.info("Creating dough for {}", recipe.getId());

        // build dough stack and write components
        ItemStack dough = new ItemStack(ModItems.DOUGH.get());
        dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                new BakerPctComponent(recipe.getTargets()));
        // build IngredientInfo list
        List<IngredientInfo> infos = new ArrayList<>();
        int total = 0;
        for (IngredientStack st : ingredientList) {
            String itemId = Optional.ofNullable(st.getBowlStack().get(ModDataComponentTypes.FLOUR_TYPE.get()))
                    .map(ft -> ft.getId())
                    .orElseGet(() ->
                            BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString()
                    );
            int grams = st.getGrams();
            total += grams;
            infos.add(new IngredientInfo(itemId, st.getCategory(), grams));
        }
        DoughRecipeComponent dr = new DoughRecipeComponent(
                recipe.getId().getPath(),
                recipe.getTargets(),
                infos,
                total
        );
        dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(), dr);
        dough.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                new WeightComponent(total));

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

        // merge or add new
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
        tag.put("Inventory", itemHandler.serializeNBT(regs));
        tag.putBoolean("Mixing", mixing);
        tag.putInt("MixProgress", mixProgress);
        tag.put("Ingredients", saveIngredientList());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        itemHandler.deserializeNBT(regs, tag.getCompound("Inventory"));
        mixing = tag.getBoolean("Mixing");
        mixProgress = tag.getInt("MixProgress");
        loadIngredientList(tag.getList("Ingredients", ListTag.TAG_COMPOUND));
    }

    private ListTag saveIngredientList() {
        ListTag list = new ListTag();
        for (IngredientStack st : ingredientList) {
            CompoundTag t = new CompoundTag();
            t.putString("Item", BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString());
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
            ItemStack stack = new ItemStack(
                    BuiltInRegistries.ITEM.get(id));
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
        SimpleContainer c = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            c.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, c);
    }
}
