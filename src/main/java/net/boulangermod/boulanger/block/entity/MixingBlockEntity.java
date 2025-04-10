package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.BakerPctComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.MixingRecipe;
import net.boulangermod.boulanger.recipe.ModMixingRecipes;
import net.boulangermod.boulanger.screen.MixingBlockMenu;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.IngredientStack;
import net.boulangermod.boulanger.component.WeightComponent;
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
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        // 1) absorb weighed bowl
        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && isWeighedIngredient(in)) {
            addIngredientFromBowl(in);
            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);
            itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL));
        }

        // 2) mixing countdown
        if (mixing) {
            mixProgress++;
            if (mixProgress >= MAX_MIX_TIME) {
                mixing = false;
                mixProgress = 0;
                generateDough();
            }
        }
    }

    public void startMixing() {
        if (!mixing && !ingredientList.isEmpty()) {
            mixing     = true;
            mixProgress = 0;
        }
    }

    private void generateDough() {
        // match a recipe by baker's percentages
        Optional<MixingRecipe> opt = matchRecipe();
        if (opt.isPresent()) {
            MixingRecipe recipe = opt.get();
            ItemStack dough = new ItemStack(recipe.resultItem());
            // attach the BakerPctComponent so client can read it
            dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                    new BakerPctComponent(recipe.targetPercentages()));
            itemHandler.setStackInSlot(OUTPUT_DOUGH, dough);
        } else {
            // no valid formula
            itemHandler.setStackInSlot(OUTPUT_DOUGH, ItemStack.EMPTY);
        }
        ingredientList.clear();
    }

    private Optional<MixingRecipe> matchRecipe() {
        if (ingredientList.isEmpty()) return Optional.empty();

        // sum grams per category
        Map<IngredientCategory, Integer> weightPerCat = new EnumMap<>(IngredientCategory.class);
        for (IngredientStack is : ingredientList) {
            weightPerCat.merge(is.category(), is.grams(), Integer::sum);
        }

        double flourW = weightPerCat.getOrDefault(IngredientCategory.FLOUR, 0);
        if (flourW <= 0) return Optional.empty();

        // compute actual baker's percentages
        Map<IngredientCategory, Double> actualPct = new EnumMap<>(IngredientCategory.class);
        for (var e : weightPerCat.entrySet()) {
            actualPct.put(e.getKey(), e.getValue() / flourW * 100.0);
        }

        // compare against each known recipe
        for (MixingRecipe recipe : ModMixingRecipes.getAll()) {
            boolean ok = true;
            for (var tgt : recipe.targetPercentages().entrySet()) {
                double got = actualPct.getOrDefault(tgt.getKey(), 0.0);
                if (Math.abs(got - tgt.getValue()) > TOLERANCE_PCT) {
                    ok = false;
                    break;
                }
            }
            if (ok) return Optional.of(recipe);
        }
        return Optional.empty();
    }

    // === existing helper methods ===

    public void addIngredientFromBowl(ItemStack bowl) {
        var item     = bowl.getItem();
        var category = IngredientCategory.getIngredientCategory(bowl);
        var weight   = bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (weight == null) return;

        for (IngredientStack stack : ingredientList) {
            if (stack.item() == item) {
                stack.addGrams((int) weight.grams());
                return;
            }
        }
        ingredientList.add(new IngredientStack(item, category, (int) weight.grams()));
    }

    private boolean isWeighedIngredient(ItemStack s) {
        return s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
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