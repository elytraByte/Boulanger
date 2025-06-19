package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.screen.DoughDividerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class DoughDividerBlockEntity extends AbstractProcessingBlockEntity {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    // grams of wiggle room
    private static final double TOLERANCE_GRAMS = 2.0;

    public DoughDividerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOUGH_DIVIDER.get(), pos, state, 2);
    }

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }


    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        // only attempt processing when we actually have dough and a valid recipe
        if (!canProcess()) return;
        processItem();
    }

    protected boolean canProcess() {
        var input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.is(ModItems.DOUGH.get())) return false;

        var recComp = input.get(ModDataComponentTypes.DOUGH_RECIPE);
        var wc      = input.get(ModDataComponentTypes.INGREDIENT_GRAMS);
        if (recComp == null || wc == null || wc.grams() <= 0) return false;

        double total = wc.grams();
        double serving = findRecipeFor(input).getServingWeightGrams();

        // 1× serving (within tolerance) OR at least 2× serving (allow a smidge under)
        boolean minimalOk = Math.abs(total - serving) <= TOLERANCE_GRAMS;
        boolean multiOk   = total >= (2 * serving - TOLERANCE_GRAMS);

        LOGGER.debug("→ Dough {}g, serving {}g → minimalOk={} multiOk={}", total, serving, minimalOk, multiOk);
        return minimalOk || multiOk;
    }

    protected void processItem() {
        // 0) Grab the input stack
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        // 0a) Nothing to do if empty or not dough
        if (input.isEmpty() || !input.is(ModItems.DOUGH.get())) {
            return;
        }

        // 1) Look up the dough‐process recipe
        DoughProcessRecipe recipe = findRecipeFor(input);
        if (recipe == null) {
            LOGGER.warn("→ processItem: no dough‐process recipe for {}", input);
            return;
        }
        double serving = recipe.getServingWeightGrams();

        // 2) Read total grams from the weight component
        WeightComponent wc = input.get(ModDataComponentTypes.INGREDIENT_GRAMS);
        double total = (wc != null ? wc.grams() : 0.0);
        int floorPortions = (int) Math.floor(total / serving);

        // --- 3) Minimal‐portion branch (≈1× serving) ---
        if (floorPortions < 2) {
            double diff = Math.abs(total - serving);
            if (diff <= TOLERANCE_GRAMS) {
                LOGGER.debug("→ {}g ≈ 1× serving ({}g ±{}g); advancing step",
                        total, serving, TOLERANCE_GRAMS);
                advanceSinglePortion(input);
            } else {
                LOGGER.debug("→ Only {} portion(s) possible and {}g off target; skipping",
                        floorPortions, diff);
            }
            return;
        }

        // --- 4) Multi‐portion branch (≥2× serving) ---
        double leftover = total - (floorPortions * serving);
        int portions;
        double portionWeight;

        if (leftover <= TOLERANCE_GRAMS) {
            // exact multiples: trim leftover
            portions      = floorPortions;
            portionWeight = serving;
            LOGGER.debug("→ {}g is {}×{}g with {}g leftover ≤{}g; trimming leftover",
                    total, portions, serving, leftover, TOLERANCE_GRAMS);
        } else {
            // split evenly
            portions      = floorPortions;
            portionWeight = total / portions;
            LOGGER.debug("→ Splitting {}g evenly into {} pieces of {}g each (no trim)",
                    total, portions, portionWeight);
        }

        // 5) Perform the divide‐and‐stamp
        divideIntoPortions(input, portions, portionWeight);
    }

    private void advanceSinglePortion(ItemStack input) {
        // ensure output empty
        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) return;

        // copy & bump proof step
        ItemStack out = input.copy();
        out.set(ModDataComponentTypes.PROOFING_STATE.get(),
                bumpProofStep(input));

        // write & clear
        itemHandler.setStackInSlot(OUTPUT_SLOT, out);
        itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    private void divideIntoPortions(ItemStack input, int portions, double pWeight) {
        // reduce input by exactly the weight we’re splitting off
        reduceInput(portions * pWeight);

        // build the output stack
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty()) {
            out = new ItemStack(input.getItem(), portions);
        } else if (out.getItem() == input.getItem()) {
            out.grow(portions);
        } else {
            LOGGER.warn("→ Cannot divide: output occupied by {}", out.getItem());
            return;
        }

        // copy everything except weight
        copyDoughMetadataExceptWeight(input, out);

        // stamp the correct recipe & weight
        var oldRec = input.get(ModDataComponentTypes.DOUGH_RECIPE);
        if (oldRec != null) {
            out.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                    scaleRecipeForWeight(oldRec, pWeight));
        }
        out.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                new WeightComponent((float)pWeight));

        // advance proof step
        out.set(ModDataComponentTypes.PROOFING_STATE.get(),
                bumpProofStep(input));

        // write back
        itemHandler.setStackInSlot(OUTPUT_SLOT, out);
        setChanged();

        LOGGER.info("→ Divided into {} × {}g", portions, pWeight);
    }

    private ProofingStateComponent bumpProofStep(ItemStack stack) {
        var old = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        int next = old != null ? old.stepIndex() + 1 : 0;
        return new ProofingStateComponent(next, /*ticks=*/0, /*shaped=*/true);
    }

    private DoughRecipeComponent scaleRecipeForWeight(DoughRecipeComponent old, double newWeight) {
        double scale = newWeight / old.totalWeight();
        var scaled = old.ingredients().stream()
                .map(i -> new IngredientInfo(i.itemId(), i.category(), (int)Math.round(i.weight() * scale)))
                .collect(Collectors.toList());
        return new DoughRecipeComponent(
                old.recipeId(),
                old.targetPercentages(),
                scaled,
                (int)newWeight
        );
    }

    private static void copyDoughMetadataExceptWeight(ItemStack src, ItemStack dst) {
        List<DataComponentType<?>> toCopy = List.of(
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.INGREDIENT_TYPE.get(),
                ModDataComponentTypes.PAN_TYPE.get()
        );

        for (DataComponentType<?> type : toCopy) {
            if (src.has(type)) {
                dst.set((DataComponentType<Object>) type, src.get(type));
            }
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.DOUGH_DIVIDER.get();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new DoughDividerMenu(id, playerInv, this);
    }

    private DoughProcessRecipe findRecipeFor(ItemStack stack) {
        // 1) do we even have a process‐type component?
        if (!stack.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) {
            return null;
        }

        // 2) pull out the ResourceLocation directly
        ResourceLocation processId =
                stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());

        // 3) stream your Process recipes, unwrap via RecipeHolder::value, then match on that ID
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get())
                .stream()
                .map(RecipeHolder::value)                            // ← RecipeHolder::value works cleanly here
                .filter(r -> r.getDoughType().equals(processId))     // or getProcessId(), depending on your API
                .findFirst()
                .orElse(null);
    }

    /**
     * Subtracts the given grams from the input dough stack.
     * If the result is ≤0, clears the slot; otherwise writes back the new weight.
     */
    private void reduceInput(double gramsToRemove) {
        // Grab the current input stack
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())) {
            LOGGER.warn("→ Tried to reduce input but no INGREDIENT_GRAMS component present.");
            return;
        }

        // Read the current weight
        WeightComponent wc = input.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        double remaining = wc.grams() - gramsToRemove;
        LOGGER.debug("→ Reducing input: {}g - {}g = {}g", wc.grams(), gramsToRemove, remaining);

        if (remaining <= 0) {
            // fully consumed
            itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
            LOGGER.debug("→ Input fully consumed; clearing slot.");
        } else {
            // update to new weight
            input.set(
                    ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                    new WeightComponent((float) remaining)
            );
            itemHandler.setStackInSlot(INPUT_SLOT, input);
            LOGGER.debug("→ Updated input with new weight: {}g", remaining);
        }

        // mark dirty so it syncs & saves
        setChanged();
    }
}