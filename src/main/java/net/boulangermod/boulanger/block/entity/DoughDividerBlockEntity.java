package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.component.*;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.DoughDividerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.boulangermod.boulanger.block.entity.BakersTableBlockEntity.copyKnownDoughComponents;

public class DoughDividerBlockEntity extends AbstractProcessingBlockEntity {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);


    public DoughDividerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOUGH_DIVIDER.get(), pos, state);// input + output
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


    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        LOGGER.debug("Tick at {}: Checking process condition", pos);

        if (canProcess()) {
            LOGGER.debug("→ Can process. Attempting to divide dough.");
            processItem();

        } else {
            LOGGER.debug("→ Cannot process: Input conditions not met.");
        }
    }

    @Override
    protected boolean canProcess() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            LOGGER.debug("→ Input slot is empty.");
            return false;
        }

        if (!input.is(ModItems.DOUGH.get())) {
            LOGGER.debug("→ Input is not a dough item: {}", input.getItem());
            return false;
        }

        DoughRecipeComponent recipeComponent = input.get(ModDataComponentTypes.DOUGH_RECIPE);
        WeightComponent weight = input.get(ModDataComponentTypes.INGREDIENT_GRAMS);

        if (recipeComponent == null) {
            LOGGER.debug("→ Missing DoughRecipeComponent.");
            return false;
        }

        if (weight == null || weight.grams() <= 0) {
            LOGGER.debug("→ Missing or invalid WeightComponent: {}", weight);
            return false;
        }

        LOGGER.debug("→ Dough has {}g, recipeId={}", weight.grams(), recipeComponent.recipeId());

        ResourceLocation processId = input.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE);
        Optional<DoughProcessRecipe> recipeOpt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(r -> (DoughProcessRecipe) r.value())
                .filter(r -> r.getDoughType().equals(processId))
                .findFirst();


        if (recipeOpt.isEmpty()) {
            LOGGER.debug("→ No DoughProcessRecipe found for: {}", recipeComponent.recipeId());
            return false;
        }

        DoughProcessRecipe processRecipe = recipeOpt.get();
        double servingWeight = processRecipe.getServingWeightGrams();
        LOGGER.debug("→ Serving weight: {}g", servingWeight);

        boolean canDivide = servingWeight > 0 && weight.grams() >= servingWeight;
        LOGGER.debug("→ canDivide = {}", canDivide);

        return canDivide;
    }

    @Override
    protected void processItem() {
        // 1) pull the input stack
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !input.has(ModDataComponentTypes.INGREDIENT_GRAMS.get())) {
            return;
        }

        // 2) find recipe & serving size
        DoughProcessRecipe recipe = findRecipeFor(input);
        if (recipe == null) return;
        double servingSize = recipe.getServingWeightGrams();

        // 3) read original weight & proof state & dough-recipe component
        WeightComponent wc           = input.get(ModDataComponentTypes.INGREDIENT_GRAMS.get());
        double          totalWeight  = wc.grams();
        ProofingStateComponent proof = input.get(ModDataComponentTypes.PROOFING_STATE.get());
        DoughRecipeComponent  origRec= input.get(ModDataComponentTypes.DOUGH_RECIPE.get());

        int   currStep = proof.stepIndex();
        int   nextStep = currStep + 1;
        boolean shaped = proof.shaped();
        int   ticks    = proof.ticksInStep();

        // 4) how many full servings?
        int portions = (int)(totalWeight / servingSize);

        // — If exactly 1 serving, auto-advance divide/proof instead of splitting —
        if (portions == 1) {
            ProcessingStep currentStep = recipe.getSteps().get(currStep);
            if (recipe.canSkipStep(input, currentStep)) {
                input.set(
                        ModDataComponentTypes.PROOFING_STATE.get(),
                        new ProofingStateComponent(nextStep, ticks, shaped)
                );
                itemHandler.setStackInSlot(INPUT_SLOT, input);
                LOGGER.debug("→ Auto-skipped divide for min-size dough, advanced to step {}", nextStep);
            }
            return;
        }

        // — If fewer than 1 serving, nothing to do —
        if (portions < 2) {
            return;
        }

        // 5) consume the input (we’ll recreate it in portions)
        itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);

        // 6) loop & emit each portion
        for (int i = 0; i < portions; i++) {
            // clone *all* metadata & NBT
            ItemStack portion = input.copy();
            portion.setCount(1);

            // a) override weight
            portion.set(
                    ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                    new WeightComponent((float) servingSize)
            );

            // b) advance proof
            portion.set(
                    ModDataComponentTypes.PROOFING_STATE.get(),
                    new ProofingStateComponent(nextStep, ticks, shaped)
            );

            // c) rebuild the DoughRecipeComponent
            List<IngredientInfo> scaledIngredients = origRec.ingredients().stream()
                    .map(info -> new IngredientInfo(
                            info.itemId(),
                            info.category(),
                            (int)Math.round(info.weight() / (double)portions)
                    ))
                    .collect(Collectors.toList());

            DoughRecipeComponent newRec = new DoughRecipeComponent(
                    origRec.recipeId(),
                    origRec.targetPercentages(),
                    scaledIngredients,
                    (int) servingSize
            );
            portion.set(ModDataComponentTypes.DOUGH_RECIPE.get(), newRec);

            // emit it
            itemHandler.insertItem(OUTPUT_SLOT, portion, false);
        }

        LOGGER.debug("→ Split {}g into {}×{}g ({}g each) and advanced proof to step {}",
                totalWeight, portions, servingSize, nextStep);
    }






    private static void copyDoughMetadataExceptWeight(ItemStack src, ItemStack dst) {
        List<DataComponentType<?>> toCopy = List.of(
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.INGREDIENT_TYPE.get()
                // <-- note: NO DOUGH_RECIPE or INGREDIENT_GRAMS here
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

