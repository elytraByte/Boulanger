package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.BakerPctComponent;
import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.IngredientInfo;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.item.FlourItemType;
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
            if (!level.isClientSide) {
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
        ModMixingRecipes.registerDefaults();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Absorb a weighed bowl if present
        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && isWeighedIngredient(in)) {
            addIngredientFromBowl(in);
            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);
            itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL));
        }

        // Mixing countdown
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
            mixing = true;
            mixProgress = 0;
        }
    }

    private void generateDough() {
        Optional<MixingRecipe> opt = matchRecipe();
        if (opt.isPresent()) {
            MixingRecipe recipe = opt.get();
            ItemStack dough = new ItemStack(recipe.resultItem());

            // Attach baker's percentages
            dough.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                    new BakerPctComponent(recipe.targetPercentages()));

            // Build IngredientInfo list
            List<IngredientInfo> infos = new ArrayList<>();
            int totalWeight = 0;
            for (IngredientStack st : ingredientList) {
                String itemId = BuiltInRegistries.ITEM
                        .getKey(st.getActualItem())
                        .toString();
                String cat = st.getCategory().name().toLowerCase();
                int grams = st.getGrams();
                totalWeight += grams;
                infos.add(new IngredientInfo(itemId, cat, grams));
            }

            // Create DoughRecipeComponent
            DoughRecipeComponent dr = new DoughRecipeComponent(
                    recipe.getId().toString(),
                    recipe.targetPercentages(),
                    infos,
                    totalWeight
            );
            dough.set(ModDataComponentTypes.DOUGH_RECIPE.get(), dr);

            itemHandler.setStackInSlot(OUTPUT_DOUGH, dough);
        } else {
            itemHandler.setStackInSlot(OUTPUT_DOUGH, ItemStack.EMPTY);
        }
        ingredientList.clear();
    }

    private Optional<MixingRecipe> matchRecipe() {
        if (ingredientList.isEmpty()) return Optional.empty();

        // Sum weights
        Map<IngredientCategory, Integer> weightPerCat = new EnumMap<>(IngredientCategory.class);
        for (var st : ingredientList) {
            weightPerCat.merge(st.getCategory(), st.getGrams(), Integer::sum);
        }
        double flourWeight = weightPerCat.getOrDefault(IngredientCategory.FLOUR, 0);
        if (flourWeight <= 0) return Optional.empty();

        // Compute actual percentages
        Map<IngredientCategory, Double> actualPct = new EnumMap<>(IngredientCategory.class);
        for (var e : weightPerCat.entrySet()) {
            actualPct.put(e.getKey(), e.getValue() / flourWeight * 100.0);
        }

        // Try each recipe
        for (MixingRecipe recipe : ModMixingRecipes.getAll()) {
            boolean ok = true;

            // 1) percentage match
            for (var tgt : recipe.targetPercentages().entrySet()) {
                double got = actualPct.getOrDefault(tgt.getKey(), 0.0);
                if (Math.abs(got - tgt.getValue()) > TOLERANCE_PCT) {
                    ok = false;
                    break;
                }
            }

            // 2) flour‑type whitelist
            if (ok) {
                Set<String> allowed = recipe.getAllowedFlourTypeIds();
                if (!allowed.isEmpty()) {
                    for (IngredientStack st : ingredientList) {
                        if (st.getCategory() == IngredientCategory.FLOUR) {
                            FlourType ft = st.getFlourType();
                            String id = ft != null ? ft.getId() : "";
                            if (!allowed.contains(id)) {
                                ok = false;
                                break;
                            }
                        }
                    }
                }
            }

            if (ok) return Optional.of(recipe);
        }

        return Optional.empty();
    }

    public void addIngredientFromBowl(ItemStack bowl) {
        IngredientCategory category = bowl.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                ? bowl.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                : IngredientCategory.getIngredientCategory(bowl);

        WeightComponent wc = bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (wc == null) return;
        int grams = (int) wc.grams();

        // merge into existing stack if same item+category
        for (IngredientStack st : ingredientList) {
            if (st.getBowlStack().getItem() == bowl.getItem()
                    && st.getCategory() == category) {
                st.addGrams(grams);
                return;
            }
        }

        // otherwise store a copy of the bowl (preserves components)
        ingredientList.add(new IngredientStack(bowl.copy()));
    }

    private boolean isWeighedIngredient(ItemStack s) {
        return s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
    }

    // NBT persistence (unchanged)
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Inventory", itemHandler.serializeNBT(regs));
        tag.putBoolean("Mixing", mixing);
        tag.putInt("MixProgress", mixProgress);
        tag.put("Ingredients", saveIngredientList());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
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
            t.putString("Item", BuiltInRegistries.ITEM
                    .getKey(st.getActualItem())
                    .toString());
            t.putString("Category", st.getCategory().name());
            t.putInt("Grams", st.getGrams());
            FlourType ft = st.getFlourType();
            if (ft != null) t.putString("FlourType", ft.getId());
            list.add(t);
        }
        return list;
    }

    private void loadIngredientList(ListTag list) {
        ingredientList.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ResourceLocation id = ResourceLocation.parse(t.getString("Item"));
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            IngredientCategory cat = IngredientCategory.valueOf(t.getString("Category"));
            int grams = t.getInt("Grams");

            stack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(grams));
            stack.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), cat);
            if (cat == IngredientCategory.FLOUR && t.contains("FlourType")) {
                FlourType ft = FlourItemType.fromId(t.getString("FlourType")).toFlourType();
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
        var c = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            c.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, c);
    }

    // GUI accessors
    public int getMixProgress()       { return mixProgress; }
    public boolean isMixing()         { return mixing; }
    public static int getMaxMixTime() { return MAX_MIX_TIME; }

    // Expose handler & ingredients for menus
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public List<IngredientStack> getIngredientList() {
        return Collections.unmodifiableList(ingredientList);
    }
}
