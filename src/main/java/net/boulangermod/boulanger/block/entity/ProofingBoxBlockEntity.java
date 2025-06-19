package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.ProofingBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

public class ProofingBoxBlockEntity extends AbstractProcessingBlockEntity {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int SLOT_COUNT = 54;
    private static final int PROOF_TIME_TICKS = 20 * 60 * 5; // 5 minutes in ticks (6000 ticks)

    public ProofingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROOFING_BOX.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.PROOFING_BOX.get();
    }


    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("proofing_box.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ProofingBoxMenu(id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ProofingBoxBlockEntity blockEntity) {
        if (level == null || level.isClientSide) return;

        for (int i = 0; i <= 26; i++) { // INPUT SLOTS ONLY
            ItemStack stack = blockEntity.itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            ResourceLocation doughType = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
            if (doughType == null) continue;

            Optional<DoughProcessRecipe> opt = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                    .map(RecipeHolder::value)
                    .filter(r -> r.getDoughType().equals(doughType))
                    .findFirst();

            if (opt.isEmpty()) continue;

            DoughProcessRecipe recipe = opt.get();
            List<ProcessingStep> steps = recipe.getSteps();
            if (steps.isEmpty()) continue;

            ProofingStateComponent proof = stack.getOrDefault(
                    ModDataComponentTypes.PROOFING_STATE.get(),
                    new ProofingStateComponent(0, 0, false)
            );

            int index = proof.stepIndex();
            if (index >= steps.size()) continue;

            ProcessingStep currentStep = steps.get(index);

            // Must be a PROOF or FINAL_PROOF step
            if (currentStep.type() != StepType.PROOF && currentStep.type() != StepType.FINAL_PROOF) continue;

            // If FINAL_PROOF, require shaped && PAN_TYPE
            if (currentStep.type() == StepType.FINAL_PROOF) {
                if (!proof.shaped() || !stack.has(ModDataComponentTypes.PAN_TYPE.get())) continue;
            }

            int ticked = proof.ticksInStep() + 1;
            if (ticked >= currentStep.durationTicks()) {
                // Advance the proofing step
                ItemStack updatedStack = stack.copy();
                updatedStack.set(ModDataComponentTypes.PROOFING_STATE.get(),
                        new ProofingStateComponent(index + 1, 0, proof.shaped()));

                // Try to move to output slot
                boolean moved = false;
                for (int j = 27; j < 54; j++) {
                    if (blockEntity.itemHandler.getStackInSlot(j).isEmpty()) {
                        blockEntity.itemHandler.setStackInSlot(j, updatedStack);
                        blockEntity.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                        LOGGER.debug("→ Dough at slot {} completed {} step and moved to output slot {}", i, currentStep.type(), j);
                        moved = true;
                        break;
                    }
                }

                if (!moved) {
                    // If no output slot available, update in-place
                    stack.set(ModDataComponentTypes.PROOFING_STATE.get(),
                            new ProofingStateComponent(index + 1, 0, proof.shaped()));
                }

            } else {
                // Still ticking in current step
                stack.set(ModDataComponentTypes.PROOFING_STATE.get(),
                        new ProofingStateComponent(index, ticked, proof.shaped()));
            }
        }
    }





    public static boolean tryPunchDown(ItemStack stack, Level level) {
        if (!stack.has(ModDataComponentTypes.PROOFING_STATE.get()) ||
                !stack.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get())) return false;

        ProofingStateComponent state = stack.get(ModDataComponentTypes.PROOFING_STATE.get());
        ResourceLocation recipeId = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());

        Optional<DoughProcessRecipe> opt = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(recipeId))
                .findFirst();

        if (opt.isEmpty()) return false;
        DoughProcessRecipe recipe = opt.get();
        List<ProcessingStep> steps = recipe.getSteps();

        if (state.stepIndex() >= steps.size()) return false;

        ProcessingStep step = steps.get(state.stepIndex());
        if (step.type() == StepType.PUNCHDOWN) {
            stack.set(ModDataComponentTypes.PROOFING_STATE.get(),
                    new ProofingStateComponent(state.stepIndex() + 1, 0, state.shaped()));
            return true;
        }

        return false;
    }

}
