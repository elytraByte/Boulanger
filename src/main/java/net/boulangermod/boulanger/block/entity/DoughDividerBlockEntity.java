package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.DoughDividerMenu;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DoughDividerBlockEntity extends AbstractProcessingBlockEntity implements AbstractProcessingBlock.Tickable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INPUT_SLOT  = 0;
    private static final int OUTPUT_SLOT = 1;
    // grams of wiggle room
    private static final double TOLERANCE_GRAMS = 2.0;

    public DoughDividerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOUGH_DIVIDER.get(), pos, state, 2);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        // only run on server
        if (level.isClientSide) return;

        // 0) Grab the input stack
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.is(ModItems.DOUGH.get())) return;

        // 1) Look up the dough-process recipe
        DoughProcessRecipe recipe = findRecipeFor(input);
        if (recipe == null) return;

        // 2) Resolve current step index safely
        int stepIdx = safeStepIndex(input, recipe);
        if (stepIdx < 0) {
            // Finished or invalid index -> nothing to do here anymore
            return;
        }

        ProcessingStep currentStep = recipe.getSteps().get(stepIdx);
        if (currentStep.getType() != StepType.DIVIDE) return;

        // 3) Only now check weight/tolerance and actually process
        if (!canProcess()) return;
        processItem();
    }

    /** Returns -1 if finished (idx >= steps.size()) or invalid; otherwise the safe index. */
    private int safeStepIndex(ItemStack stack, DoughProcessRecipe recipe) {
        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE);
        int idx = (proof != null ? proof.stepIndex() : 0);
        int size = recipe.getSteps().size();
        if (idx < 0) idx = 0;
        if (idx >= size) {
            // Already beyond last step => complete
            return -1;
        }
        return idx;
    }

    protected boolean canProcess() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.is(ModItems.DOUGH.get())) return false;

        // a) recipe must exist
        DoughProcessRecipe recipe = findRecipeFor(input);
        if (recipe == null) return false;

        // b) must be on a valid step and that step must be DIVIDE
        int stepIdx = safeStepIndex(input, recipe);
        if (stepIdx < 0) return false; // finished/invalid
        ProcessingStep currentStep = recipe.getSteps().get(stepIdx);
        if (currentStep.getType() != StepType.DIVIDE) return false;

        // c) weight checks
        WeightComponent wc = input.get(ModDataComponentTypes.INGREDIENT_GRAMS);
        if (wc == null || wc.grams() <= 0) return false;

        double total   = wc.grams();
        double serving = recipe.getServingWeightGrams();
        if (serving <= 0) {
            return false;
        }

        boolean minimalOk = Math.abs(total - serving) <= TOLERANCE_GRAMS;
        boolean multiOk   = total >= (2 * serving - TOLERANCE_GRAMS);
        return minimalOk || multiOk;
    }

    protected void processItem() {
        // 0) Grab the input stack
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.is(ModItems.DOUGH.get())) return;

        // 1) Look up the dough‐process recipe
        DoughProcessRecipe recipe = findRecipeFor(input);
        if (recipe == null) {
            return;
        }
        double serving = recipe.getServingWeightGrams();
        if (serving <= 0) {
            return;
        }

        // 2) Read total grams from the weight component
        WeightComponent wc = input.get(ModDataComponentTypes.INGREDIENT_GRAMS);
        double total = (wc != null ? wc.grams() : 0.0);
        int floorPortions = (int) Math.floor(total / serving);

        // --- 3) Minimal‐portion branch (≈1× serving) ---
        if (floorPortions < 2) {
            double diff = Math.abs(total - serving);
            if (diff <= TOLERANCE_GRAMS) {
                advanceSinglePortion(input);
            } else {
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
        } else {
            // split evenly
            portions      = floorPortions;
            portionWeight = total / portions;
        }

        if (portions <= 0 || portionWeight <= 0) {
            return;
        }

        // 5) Perform the divide‐and‐stamp
        divideIntoPortions(input, portions, portionWeight);
    }

    private void advanceSinglePortion(ItemStack input) {
        // ensure output empty
        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) return;

        // copy & bump proof step
        ItemStack out = input.copy();
        out.set(ModDataComponentTypes.PROOFING_STATE.get(), bumpProofStep(input));

        // write & clear
        itemHandler.setStackInSlot(OUTPUT_SLOT, out);
        itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    /**
     * Divide input into up to {@code portions} items of {@code pWeight} each,
     * respecting output stack capacity. Only reduces input after confirming output is writable.
     */
    private void divideIntoPortions(ItemStack input, int portions, double pWeight) {
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);

        // Incompatible occupant?
        if (!out.isEmpty() && out.getItem() != input.getItem()) {
            return;
        }

        // How many can the output stack accept?
        int maxStack = input.getMaxStackSize();
        int existing = (!out.isEmpty() ? out.getCount() : 0);
        int capacity = Math.max(0, maxStack - existing);
        if (capacity <= 0) {
            return;
        }

        int produce = Math.min(portions, capacity);
        if (produce <= 0) return;

        // Build/extend the output stack first (so we don't destroy input if we can't output)
        if (out.isEmpty()) {
            out = new ItemStack(input.getItem(), produce);
        } else {
            out.grow(produce);
        }

        // Copy everything except weight
        copyDoughMetadataExceptWeight(input, out);

        // Stamp the correct recipe & per-piece weight
        var oldRec = input.get(ModDataComponentTypes.DOUGH_RECIPE);
        if (oldRec != null) {
            out.set(ModDataComponentTypes.DOUGH_RECIPE.get(),
                    scaleRecipeForWeight(oldRec, pWeight));
        }
        out.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent((float) pWeight));

        // Advance proof step for the produced portions
        out.set(ModDataComponentTypes.PROOFING_STATE.get(), bumpProofStep(input));

        // Write output
        itemHandler.setStackInSlot(OUTPUT_SLOT, out);

        // Now reduce input by the actually produced amount
        double gramsUsed = produce * pWeight;
        reduceInput(gramsUsed);

        setChanged();
    }

    private ProofingStateComponent bumpProofStep(ItemStack stack) {
        var old  = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        int next = (old != null ? old.stepIndex() + 1 : 0);
        // Never mark shaped here; preserve whatever it was (spawned dough: false)
        boolean shaped = (old != null && old.shaped());
        return new ProofingStateComponent(next, /*ticks=*/0, /*shaped=*/shaped);
    }




    private DoughRecipeComponent scaleRecipeForWeight(DoughRecipeComponent old, double newWeightGrams) {
        // Total mg from the precise per-ingredient list
        int oldTotalMg = old.ingredients().stream()
                .mapToInt(IngredientInfo::milligrams)
                .sum();

        // Target total in mg
        int newTotalMg = (int) Math.round(newWeightGrams * 1000.0);

        // Fallback if an older item somehow had 0 mg in the list
        double scale = oldTotalMg > 0
                ? (newTotalMg / (double) oldTotalMg)
                : (newWeightGrams / Math.max(1, old.totalWeight())); // legacy grams-based fallback

        var scaled = old.ingredients().stream()
                .map(i -> {
                    int mg = (int) Math.round(i.milligrams() * scale);
                    IngredientInfo out = IngredientInfo.ofMg(i.itemId(), i.category(), mg);
                    if (i.category() == IngredientCategory.FLOUR && i.flourType() != null) {
                        out = out.withFlourType(i.flourType());
                    }
                    return out;
                })
                .collect(java.util.stream.Collectors.toList());

        // Keep component's top-level total as whole grams (rounded from mg)
        int totalGramsRounded = Math.round(newTotalMg / 1000f);

        return new DoughRecipeComponent(
                old.recipeId(),
                old.targetPercentages(),
                scaled,
                totalGramsRounded
        );
    }


    @SuppressWarnings("unchecked")
    private static void copyDoughMetadataExceptWeight(ItemStack src, ItemStack dst) {
        // Intentionally NOT copying PAN_TYPE here; the pan item should supply that later.
        List<DataComponentType<?>> toCopy = List.of(
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.INGREDIENT_TYPE.get()
        );

        for (DataComponentType<?> type : toCopy) {
            if (src.has(type)) {
                dst.set((DataComponentType<Object>) type, src.get(type));
            }
        }
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
        ResourceLocation processId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());

        // 3) stream your Process recipes, unwrap via RecipeHolder::value, then match on that ID
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(processId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Subtracts the given grams from the input dough stack.
     * If the result is ≤0, clears the slot; otherwise writes back the new weight.
     */
    private void reduceInput(double gramsToRemove) {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())) {
            return;
        }

        WeightComponent wc = input.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        double remaining = wc.grams() - gramsToRemove;

        if (remaining <= 0) {
            itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        } else {
            input.set(ModDataComponentTypes.INGREDIENT_GRAMS.get(), new WeightComponent((float) remaining));
            itemHandler.setStackInSlot(INPUT_SLOT, input);
        }

        setChanged();
    }
}
