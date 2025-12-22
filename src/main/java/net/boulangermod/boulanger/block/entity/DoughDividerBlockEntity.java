package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.*;
import net.boulangermod.boulanger.screen.DoughDividerMenu;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
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

    private static final int INPUT_SLOT  = 0;
    private static final int OUTPUT_SLOT = 1;
    // grams of wiggle room
    private static final double TOLERANCE_GRAMS = 2.0;

    /** If true → ROLL mode; false → LOAF mode. */
    private boolean rollMode = false;
    private boolean modeSelected = false;

    public DoughDividerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOUGH_DIVIDER.get(), pos, state, 2);
    }

    public void setRollMode(boolean roll) {
        // player explicitly chose a mode
        this.modeSelected = true;

        if (this.rollMode == roll) {
            // still changed: we want clients to update from neutral → pressed
            setChanged();
            syncToClient();
            return;
        }

        this.rollMode = roll;
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level == null) return;
        var state = getBlockState();
        level.sendBlockUpdated(this.worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("RollMode", this.rollMode);
        tag.putBoolean("ModeSelected", this.modeSelected);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        this.rollMode = tag.getBoolean("RollMode");
        this.modeSelected = tag.getBoolean("ModeSelected"); // old worlds: defaults to false
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

        ProcessingStep currentStep = recipe.steps().get(stepIdx);
        if (currentStep.type() != StepType.DIVIDE) return;

        // 3) Only now check weight/tolerance and actually process
        if (!canProcess()) return;
        processItem();
    }

    /** Returns -1 if finished (idx >= steps.size()) or invalid; otherwise the safe index. */
    private int safeStepIndex(ItemStack stack, DoughProcessRecipe recipe) {
        ProofingStateComponent proof = stack.get(ModDataComponentTypes.PROOFING_STATE);
        int idx = (proof != null ? proof.stepIndex() : 0);
        int size = recipe.steps().size();
        if (idx < 0) idx = 0;
        if (idx >= size) {
            // Already beyond last step => complete
            return -1;
        }
        return idx;
    }

    // Returns true if the divider should run this tick.
    protected boolean canProcess() {
        // 0) Input must be dough
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.is(ModItems.DOUGH.get())) return false;

        // 1) Must be at DIVIDE step
        ProofingStateComponent ps = input.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation procId   = input.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
        if (ps == null || procId == null) return false;

        DoughProcessRecipe recipe = findRecipeFor(input);
        if (recipe == null) return false;

        int idx = ps.stepIndex();
        List<ProcessingStep> steps = recipe.steps();
        if (idx < 0 || idx >= steps.size()) return false;
        if (steps.get(idx).type() != StepType.DIVIDE) return false;

        // 2) Compute serving weight safely from the DOUGH stack
        double serving = getTargetServingWeight(input);
        if (serving <= 0) return false;

        // 3) Total grams must be positive
        var wc = input.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        double total = (wc != null ? wc.grams() : 0.0);
        if (total <= 0) return false;

        // 4) Either ≈1 serving (within tolerance) or ≥2 servings
        int floorPortions = (int)Math.floor(total / serving);
        boolean canSingle = floorPortions < 2 && Math.abs(total - serving) <= TOLERANCE_GRAMS;
        boolean canMulti  = floorPortions >= 2;
        if (!(canSingle || canMulti)) return false;

        // 5) Output must be free
        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) return false;

        return true;
    }

    protected void processItem() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.is(ModItems.DOUGH.get())) return;

        DoughProcessRecipe recipe = findRecipeFor(input);
        if (recipe == null) return;

        double serving = getTargetServingWeight(input);
        if (serving <= 0) return;

        var wc = input.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        double total = (wc != null ? wc.grams() : 0.0);
        if (total <= 0) return;

        int floorPortions = (int)Math.floor(total / serving);

        if (floorPortions < 2) {
            if (Math.abs(total - serving) <= TOLERANCE_GRAMS) {
                advanceSinglePortion(input);
            }
            return;
        }

        double leftover = total - (floorPortions * serving);
        int portions = floorPortions;
        double portionWeight = (leftover <= TOLERANCE_GRAMS) ? serving : (total / portions);

        if (portions <= 0 || portionWeight <= 0) return;

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

        // NEW: pass process id as 2nd arg
        return new DoughRecipeComponent(
                old.recipeId(),
                resolveProcessId(old),                 // <- add this
                old.targetPercentages(),
                scaled,
                totalGramsRounded
        );
    }

    /** Prefer old.processId() if present; otherwise fall back to "<base>_process". */
    private static ResourceLocation resolveProcessId(DoughRecipeComponent comp) {
        try {
            // supports either record accessor 'processId()' or getter 'getProcessId()'
            try {
                var m = DoughRecipeComponent.class.getMethod("processId");
                Object v = m.invoke(comp);
                if (v instanceof ResourceLocation rl) return rl;
                if (v instanceof String s) return ResourceLocation.parse(s);
            } catch (NoSuchMethodException ignore) {
                var m = DoughRecipeComponent.class.getMethod("getProcessId");
                Object v = m.invoke(comp);
                if (v instanceof ResourceLocation rl) return rl;
                if (v instanceof String s) return ResourceLocation.parse(s);
            }
        } catch (Throwable ignored) {}

        ResourceLocation base = comp.recipeId();
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_process");
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
                .getAllRecipesFor(ModRecipeTypes.DOUGH_PROCESS.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getType().equals(processId))
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

    // Returns desired grams of ONE divided piece. 0 = unknown (don’t run).
    private int getTargetServingWeight(ItemStack dough) {
        if (dough == null || dough.isEmpty() || this.level == null) return 0;

        // Current dough total grams (used as last-resort fallback)
        var gramsComp = dough.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        final int totalG = (gramsComp != null) ? (int) Math.round(gramsComp.grams()) : 0;

        // Prefer serving sizes defined on the base ratio recipe.
        var ratio = findRatioRecipeFor(dough);
        Integer rollI = null, loafI = null;
        if (ratio != null) {
            rollI = ratio.getRollSizeG();  // may be null
            loafI = ratio.getLoafSizeG();  // may be null
        }

        // If the player explicitly chose a mode, honor it first.
        if (this.modeSelected) {
            Integer preferred = this.rollMode ? rollI : loafI;
            Integer fallback  = this.rollMode ? loafI : rollI;
            if (preferred != null && preferred > 0) return preferred;
            if (fallback  != null && fallback  > 0) return fallback;
        } else {
            // No explicit choice: if only one is defined, use it; if both, prefer loaf by default.
            if (loafI != null && loafI > 0) return loafI;
            if (rollI != null && rollI > 0) return rollI;
        }

        // Last resort: treat current dough as a single piece;
        // (lets the machine “nudge” one serving even without recipe sizes)
        return totalG > 0 ? totalG : 0;
    }



    /** Find the base RatioRecipe that produced this dough (via DoughRecipeComponent.recipeId). */
    private RatioRecipe findRatioRecipeFor(ItemStack stack) {
        if (this.level == null) return null;
        var dr = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr == null) return null;

        return this.level.getRecipeManager()
                .byKey(dr.recipeId())
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(net.boulangermod.boulanger.recipe.RatioRecipe.class::isInstance)
                .map(net.boulangermod.boulanger.recipe.RatioRecipe.class::cast)
                .orElse(null);
    }

}
