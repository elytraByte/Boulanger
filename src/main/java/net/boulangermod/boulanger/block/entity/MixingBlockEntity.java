package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.component.*;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class MixingBlockEntity extends BlockEntity implements AbstractProcessingBlock.Tickable, MenuProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int INPUT_BOWL   = 0;
    public static final int OUTPUT_BOWL  = 1;
    public static final int OUTPUT_DOUGH = 2;


    public ItemStackHandler getItemHandler() { return itemHandler; }
    public List<IngredientStack> getIngredientList() { return Collections.unmodifiableList(ingredientList); }
    public int getMixProgress() { return mixProgress; }
    public boolean isMixing()     { return mixing; }
    public static int getMaxMixTime()  { return MAX_MIX_TIME; }

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

        ItemStack in = itemHandler.getStackInSlot(INPUT_BOWL);
        if (!in.isEmpty() && isWeighedIngredient(in)) {
            // 1) add your ingredient to the list
            addIngredientFromBowl(in);

            // 2) clear the input
            itemHandler.setStackInSlot(INPUT_BOWL, ItemStack.EMPTY);

            // 3) spawn an empty bowl in OUTPUT_BOWL, stacking if possible
            ItemStack out = itemHandler.getStackInSlot(OUTPUT_BOWL);
            if (out.isEmpty()) {
                // nothing there yet → place one bowl
                itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL));
            } else if (out.getItem() == Items.BOWL) {
                // already a stack of bowls → grow it
                out.grow(1);
                itemHandler.setStackInSlot(OUTPUT_BOWL, out);
            } else {
                // somehow another item is in that slot → overwrite
                itemHandler.setStackInSlot(OUTPUT_BOWL, new ItemStack(Items.BOWL));
            }
        }

        // ... rest of your mixing logic unchanged ...
    }


    public void startMixing() {
        if (ingredientList.isEmpty()) {
            LOGGER.warn("Debug: crafting default whole wheat bread dough");
            Optional<MixingRecipe> debugRec = ModMixingRecipes.getAll().stream()
                    .filter(r -> r.getId().getPath().equals("whole_wheat_bread"))
                    .findFirst();
            debugRec.ifPresent(this::craftDebugDough);
            return;
        }
        if (!mixing) {
            mixing = true;
            mixProgress = 0;
        }
    }

    private void craftDebugDough(MixingRecipe recipe) {
        ItemStack doughStack = new ItemStack(ModItems.DOUGH.get());
        doughStack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), new BakerPctComponent(recipe.targetPercentages()));
        DoughRecipeComponent dr = new DoughRecipeComponent(recipe.getId().toString(), recipe.targetPercentages(), List.of(), 0);
        doughStack.set(ModDataComponentTypes.DOUGH_RECIPE.get(), dr);
        doughStack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(0));
        itemHandler.setStackInSlot(OUTPUT_DOUGH, doughStack);
    }

    private void generateDough() {
        LOGGER.debug("Generating dough at {}", worldPosition);
        Optional<MixingRecipe> opt = matchRecipe();
        if (opt.isEmpty()) {
            LOGGER.info("No valid recipe found, clearing ingredients");
            itemHandler.setStackInSlot(OUTPUT_DOUGH, ItemStack.EMPTY);
            ingredientList.clear();
            return;
        }

        MixingRecipe recipe = opt.get();
        LOGGER.info("Matched recipe {}", recipe.getId());

        ItemStack doughStack = new ItemStack(ModItems.DOUGH.get());
        doughStack.set(ModDataComponentTypes.BAKER_PERCENTAGES.get(), new BakerPctComponent(recipe.targetPercentages()));

        List<IngredientInfo> infos = new ArrayList<>();
        int totalWeight = 0;

        for (IngredientStack st : ingredientList) {
            ItemStack bowl = st.getBowlStack();
            var ft = bowl.get(ModDataComponentTypes.FLOUR_TYPE.get());
            String itemId = ft != null ? ft.getId() : BuiltInRegistries.ITEM.getKey(st.getActualItem()).toString();

            // Properly set the category from the IngredientStack itself
            IngredientCategory category = st.getCategory();

            int grams = st.getGrams();
            totalWeight += grams;

            infos.add(new IngredientInfo(itemId, category, grams));
        }

        DoughRecipeComponent dr = new DoughRecipeComponent(
                recipe.getId().toString(),
                recipe.targetPercentages(),
                infos,
                totalWeight
        );
        doughStack.set(ModDataComponentTypes.DOUGH_RECIPE.get(), dr);
        doughStack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(totalWeight));

        itemHandler.setStackInSlot(OUTPUT_DOUGH, doughStack);
        ingredientList.clear();
    }


    private Optional<MixingRecipe> matchRecipe() {
        if (ingredientList.isEmpty()) return Optional.empty();

        LOGGER.debug("---- Mixer @{} contents ----", this.worldPosition);
        for (IngredientStack st : ingredientList) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(st.getActualItem());
            LOGGER.debug("  • {} → {}g (category {})", itemId, st.getGrams(), st.getCategory());
        }

        Map<IngredientCategory, Integer> weightPerCat = new EnumMap<>(IngredientCategory.class);
        for (var st : ingredientList) {
            weightPerCat.merge(st.getCategory(), st.getGrams(), Integer::sum);
        }
        LOGGER.debug("  Category totals: {}", weightPerCat);

        double flourWeight = weightPerCat.getOrDefault(IngredientCategory.FLOUR, 0);
        if (flourWeight <= 0) {
            LOGGER.warn("  No flour present, cannot match any recipe");
            return Optional.empty();
        }

        Map<IngredientCategory, Double> actualPct = new EnumMap<>(IngredientCategory.class);
        for (var e : weightPerCat.entrySet()) {
            actualPct.put(e.getKey(), e.getValue() / flourWeight * 100.0);
        }
        LOGGER.debug("  Actual baker's %: {}", actualPct);

        for (MixingRecipe recipe : ModMixingRecipes.getAll()) {
            LOGGER.debug("-- Testing recipe {} targets={} --", recipe.getId(), recipe.targetPercentages());
            boolean ok = true;
            for (var tgt : recipe.targetPercentages().entrySet()) {
                double got = actualPct.getOrDefault(tgt.getKey(), 0.0);
                if (Math.abs(got - tgt.getValue()) > TOLERANCE_PCT) {
                    ok = false;
                    LOGGER.debug("    • {} off by {}%", tgt.getKey(), got - tgt.getValue());
                    break;
                }
            }
            if (ok && !recipe.getAllowedFlourTypeIds().isEmpty()) {
                for (IngredientStack st : ingredientList) {
                    if (st.getCategory() == IngredientCategory.FLOUR) {
                        String id = st.getFlourType() != null ? st.getFlourType().getId() : "<none>";
                        if (!recipe.getAllowedFlourTypeIds().contains(id)) {
                            ok = false;
                            LOGGER.debug("    • Flour type {} not allowed", id);
                            break;
                        }
                    }
                }
            }
            if (ok) {
                LOGGER.info("🔗 Mixer matched recipe {}", recipe.getId());
                return Optional.of(recipe);
            }
        }

        LOGGER.warn("⚠ Mixer did not match any recipe for these contents");
        return Optional.empty();
    }

    public void addIngredientFromBowl(ItemStack bowl) {
        // 1) figure out the category & weight
        IngredientCategory category = bowl.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                ? bowl.get(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                : IngredientCategory.getIngredientCategory(bowl);
        WeightComponent wc = bowl.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        if (wc == null) return;
        int grams = (int) wc.grams();

        // 2) pull out the flour‑type if it’s a flour bowl
        FlourType newFt = bowl.has(ModDataComponentTypes.FLOUR_TYPE.get())
                ? bowl.get(ModDataComponentTypes.FLOUR_TYPE.get())
                : null;

        // 3) try to merge into an existing stack
        for (IngredientStack st : ingredientList) {
            if (st.getCategory() != category) continue;

            if (category == IngredientCategory.FLOUR) {
                // only merge flours of the same FlourType
                FlourType existingFt = st.getFlourType();
                if (Objects.equals(existingFt, newFt)) {
                    st.addGrams(grams);
                    return;
                }
            } else {
                // for non‑flour, merge all of the same category
                st.addGrams(grams);
                return;
            }
        }

        // 4) no match → start a brand‑new entry
        ingredientList.add(new IngredientStack(bowl.copy()));
    }

    private boolean isWeighedIngredient(ItemStack s) {
        return s.has(ModDataComponentTypes.INGREDIENT_CATEGORY.get())
                && s.has(ModDataComponentTypes.INGREDIENT_GRAMS.get());
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
            ResourceLocation id = ResourceLocation.parse(t.getString("Item"));
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            IngredientCategory cat = IngredientCategory.valueOf(t.getString("Category"));
            int grams = t.getInt("Grams");
            stack.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent(grams));
            stack.set(ModDataComponentTypes.INGREDIENT_CATEGORY.get(), cat);
            if (cat == IngredientCategory.FLOUR && t.contains("FlourType")) {
                var ft = FlourItemType.fromId(t.getString("FlourType")).toFlourType();
                stack.set(ModDataComponentTypes.FLOUR_TYPE.get(), ft);
            }
            ingredientList.add(new IngredientStack(stack));
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("mixing_block.boulanger"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new MixingBlockMenu(id, inv, this); }
    @Override public void drops() {
        SimpleContainer c = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            c.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, c);
    }

}
