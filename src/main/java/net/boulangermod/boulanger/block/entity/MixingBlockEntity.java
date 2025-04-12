package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.MixingRecipe;
import net.boulangermod.boulanger.recipe.ModMixingRecipes;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

public class MixingBlockEntity extends BlockEntity implements AbstractProcessingBlock.Tickable, MenuProvider {
    public static final int INPUT_BOWL   = 0;
    public static final int OUTPUT_BOWL  = 1;
    public static final int OUTPUT_DOUGH = 2;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
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
        // ensure recipes exist
        ModMixingRecipes.registerDefaults();
        System.out.println("[DEBUG] MixingBlockEntity initialized at " + pos);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        // Debug: tick start
        System.out.println("[DEBUG] Tick start - Mixing: " + mixing + ", MixProgress: " + mixProgress);

        // 1) absorb weighed bowl
        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && isWeighedIngredient(in)) {
            System.out.println("[DEBUG] Found weighed bowl: " + in);
            addIngredientFromBowl(in);
            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);
            itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL));
            System.out.println("[DEBUG] Bowl processed and cleared");
        }

        // 2) mixing countdown
        if (mixing) {
            mixProgress++;
            System.out.println("[DEBUG] Mixing in progress. Current progress: " + mixProgress);
            if (mixProgress >= MAX_MIX_TIME) {
                System.out.println("[DEBUG] Mix complete, generating dough...");
                mixing = false;
                mixProgress = 0;
                generateDough();
            }
        }
    }

    public void startMixing() {
        if (!mixing && !ingredientList.isEmpty()) {
            mixing = true;
            mixProgress = 0;
            System.out.println("[DEBUG] Starting mixing with ingredients: " + ingredientList);
        }
    }

    private void generateDough() {
        // match a recipe by baker's percentages
        Optional<MixingRecipe> opt = matchRecipe();
        if (opt.isPresent()) {
            MixingRecipe recipe = opt.get();
            System.out.println("[DEBUG] Recipe matched: " + recipe.toString());
            ItemStack dough = new ItemStack(recipe.resultItem());

            // Attach BakerPctComponent as before.
            dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), new BakerPctComponent(recipe.targetPercentages()));

            // Convert the ingredientList (List<IngredientStack>) to a list of IngredientInfo records.
            List<IngredientInfo> ingredientInfos = new ArrayList<>();
            int totalWeight = 0;
            for (IngredientStack stack : ingredientList) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.item()).toString();
                String category = stack.category().name();
                int weight = stack.grams();
                totalWeight += weight;
                ingredientInfos.add(new IngredientInfo(itemId, category, weight));
            }

            // Create a DoughRecipeComponent with the extra recipe information.
            DoughRecipeComponent doughRecipe = new DoughRecipeComponent(
                    recipe.getId().toString(),      // recipe name, assumed to be a ResourceLocation converted to string
                    recipe.targetPercentages(),       // target percentages
                    ingredientInfos,                  // individual ingredient data
                    totalWeight                       // total weight of ingredients
            );

            // Attach the DoughRecipeComponent using Neoforge data components.
            dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(), doughRecipe);

            itemHandler.setStackInSlot(OUTPUT_DOUGH, dough);
            System.out.println("[DEBUG] Dough generated with recipe data: " + dough);
        } else {
            System.out.println("[DEBUG] No matching recipe found. Dough slot cleared.");
            itemHandler.setStackInSlot(OUTPUT_DOUGH, ItemStack.EMPTY);
        }
        ingredientList.clear();
    }



    private Optional<MixingRecipe> matchRecipe() {
        if (ingredientList.isEmpty()) return Optional.empty();

        // Sum the measured weight per category.
        Map<IngredientCategory, Integer> weightPerCat = new EnumMap<>(IngredientCategory.class);
        for (IngredientStack is : ingredientList) {
            weightPerCat.merge(is.category(), is.grams(), Integer::sum);
        }
        System.out.println("[DEBUG] Weight per category: " + weightPerCat);

        // Use flour weight as the baseline.
        double flourWeight = weightPerCat.getOrDefault(IngredientCategory.FLOUR, 0);
        if (flourWeight <= 0) {
            System.out.println("[DEBUG] No flour detected. Cannot match recipe.");
            return Optional.empty();
        }
        System.out.println("[DEBUG] Flour weight: " + flourWeight);

        // Compute percentages, where each non-flour category is a percentage of flour weight.
        Map<IngredientCategory, Double> actualPct = new EnumMap<>(IngredientCategory.class);
        for (var e : weightPerCat.entrySet()) {
            double pct = e.getValue() / flourWeight * 100.0;
            actualPct.put(e.getKey(), pct);
        }
        System.out.println("[DEBUG] Computed percentages: " + actualPct);

        // Compare these computed percentages against the target percentages defined in your recipes.
        for (MixingRecipe recipe : ModMixingRecipes.getAll()) {
            boolean ok = true;
            System.out.println("[DEBUG] Comparing to recipe: " + recipe.toString() + " with target percentages: " + recipe.targetPercentages());
            for (var tgt : recipe.targetPercentages().entrySet()) {
                double got = actualPct.getOrDefault(tgt.getKey(), 0.0);
                if (Math.abs(got - tgt.getValue()) > TOLERANCE_PCT) {
                    ok = false;
                    System.out.println("[DEBUG] Mismatch for " + tgt.getKey() + ": got " + got + ", expected " + tgt.getValue());
                    break;
                }
            }
            if (ok) {
                System.out.println("[DEBUG] Recipe " + recipe.toString() + " matched successfully.");
                return Optional.of(recipe);
            }
        }
        System.out.println("[DEBUG] No matching recipe after comparisons.");
        return Optional.empty();
    }

    // === existing helper methods ===

    public void addIngredientFromBowl(ItemStack bowl) {
        Item item = bowl.getItem();
        IngredientCategory category = null;

        // First, try to read the ingredient_category data component that the scale should have attached.
        if (bowl.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())) {
            category = bowl.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get());
            System.out.println("[DEBUG] Retrieved ingredient category from data component: " + category);
        }

        // If no data component is present, fall back to the computed category.
        if (category == null) {
            category = IngredientCategory.getIngredientCategory(bowl);
            System.out.println("[DEBUG] Computed ingredient category (fallback): " + category);
        }

        // Get the measured weight from the WeightComponent.
        WeightComponent weight = bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (weight == null) {
            System.out.println("[DEBUG] No weight component found. Ignoring ingredient.");
            return;
        }
        int grams = (int) weight.grams();
        System.out.println("[DEBUG] Adding ingredient from bowl. Item: " + item + ", Category: " + category + ", Weight: " + grams);

        // Check if an ingredient for this item and category already exists; if so, update its weight.
        for (IngredientStack stack : ingredientList) {
            if (stack.item() == item && stack.category() == category) {
                stack.addGrams(grams);
                System.out.println("[DEBUG] Updated existing ingredient stack: " + stack);
                return;
            }
        }

        // If not, add a new ingredient stack.
        IngredientStack newStack = new IngredientStack(item, category, grams);
        ingredientList.add(newStack);
        System.out.println("[DEBUG] New ingredient stack added: " + newStack);
    }

    private boolean isWeighedIngredient(ItemStack s) {
        boolean result = s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        System.out.println("[DEBUG] isWeighedIngredient check for " + s + ": " + result);
        return result;
    }

    public List<IngredientStack> getIngredientList() {
        return ingredientList;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void drops() {
        var container = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            container.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("mixing_block.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MixingBlockMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("Mixing", this.mixing);
        tag.putInt("MixProgress", this.mixProgress);
        tag.put("Ingredients", saveIngredientList());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.mixing = tag.getBoolean("Mixing");
        this.mixProgress = tag.getInt("MixProgress");
        loadIngredientList(tag.getList("Ingredients", CompoundTag.TAG_COMPOUND));
    }

    private ListTag saveIngredientList() {
        ListTag listTag = new ListTag();
        for (IngredientStack stack : ingredientList) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Item", BuiltInRegistries.ITEM.getKey(stack.item()).toString());
            tag.putString("Category", stack.category().name());
            tag.putInt("Grams", stack.grams());
            listTag.add(tag);
        }
        return listTag;
    }

    private void loadIngredientList(ListTag listTag) {
        ingredientList.clear();
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag tag = listTag.getCompound(i);
            ResourceLocation id = ResourceLocation.parse(tag.getString("Item"));
            Item item = BuiltInRegistries.ITEM.get(id);
            IngredientCategory category = IngredientCategory.valueOf(tag.getString("Category"));
            int grams = tag.getInt("Grams");
            ingredientList.add(new IngredientStack(item, category, grams));
        }
    }

    public int getMixProgress() {
        return mixProgress;
    }

    public boolean isMixing() {
        return mixing;
    }

    public static int getMaxMixTime() {
        return MAX_MIX_TIME;
    }
}
