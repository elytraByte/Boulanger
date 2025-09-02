package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.ProofingBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

public class ProofingBoxBlockEntity extends AbstractProcessingBlockEntity implements AbstractProcessingBlock.Tickable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int SLOT_COUNT = 5;

    public ProofingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROOFING_BOX.get(), pos, state, SLOT_COUNT);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("proofing_box.boulanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ProofingBoxMenu(id, playerInventory, this);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide) return;

        final int slots = this.itemHandler.getSlots(); // now 5
        for (int i = 0; i < slots; i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
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
            if (index >= steps.size()) continue; // already finished

            ProcessingStep current = steps.get(index);
            // only tick PROOF / FINAL_PROOF
            if (current.type() != StepType.PROOF && current.type() != StepType.FINAL_PROOF) continue;

            // FINAL_PROOF requires shaped + pan
            if (current.type() == StepType.FINAL_PROOF) {
                if (!proof.shaped() || !stack.has(ModDataComponentTypes.PAN_TYPE.get())) continue;
            }

            int ticked = proof.ticksInStep() + 1;
            if (ticked >= current.durationTicks()) {
                // advance to next step, IN PLACE
                ItemStack updated = stack.copy();
                updated.set(ModDataComponentTypes.PROOFING_STATE.get(),
                        new ProofingStateComponent(index + 1, 0, proof.shaped()));
                this.itemHandler.setStackInSlot(i, updated);
                LOGGER.debug("Slot {} advanced {} -> step {}", i, current.type(), index + 1);
            } else {
                // continue ticking this step, IN PLACE
                ItemStack updated = stack.copy();
                updated.set(ModDataComponentTypes.PROOFING_STATE.get(),
                        new ProofingStateComponent(index, ticked, proof.shaped()));
                this.itemHandler.setStackInSlot(i, updated);
            }
        }
    }

}
