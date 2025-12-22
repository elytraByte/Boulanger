package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.DoughRecipeComponent;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.item.PanItem;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeTypes;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.ProofingBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
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

        boolean changed = false;

        final int slots = this.itemHandler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // Case 1: pan with dough pieces inside
            if (stack.getItem() instanceof PanItem) {
                if (tickPanStack(level, stack)) {
                    // write back and mark changed
                    this.itemHandler.setStackInSlot(i, stack);
                    changed = true;
                }
                continue;
            }

            // Case 2: loose dough item (single piece)
            DoughProcessRecipe recipe = resolveProcessForStack(level, stack);
            if (recipe == null) continue;

            if (tickDoughPiece(stack, recipe, /*requirePanForFinal=*/false)) {
                this.itemHandler.setStackInSlot(i, stack);
                changed = true;
            }
        }

        if (changed) setChanged();
    }

    /** Tick all dough pieces inside a pan stack. Returns true if any piece changed. */
    private boolean tickPanStack(Level level, ItemStack pan) {
        boolean anyChange = false;

        // Normalize/ensure the container exists/sized per pan spec
        PanItem.ensureContainerSized(pan);
        ItemContainerContents cont = pan.get(DataComponents.CONTAINER);
        if (cont == null) return false;

        int n = cont.getSlots();
        if (n <= 0) return false;

        NonNullList<ItemStack> list = NonNullList.withSize(n, ItemStack.EMPTY);
        for (int s = 0; s < n; s++) list.set(s, cont.getStackInSlot(s));

        for (int s = 0; s < n; s++) {
            ItemStack piece = list.get(s);
            if (piece.isEmpty()) continue;

            DoughProcessRecipe recipe = resolveProcessForStack(level, piece);
            if (recipe == null) continue;

            if (tickDoughPiece(piece, recipe, /*requirePanForFinal=*/true)) {
                list.set(s, piece);
                anyChange = true;
            }
        }

        if (anyChange) {
            pan.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
            PanItem.syncModelToContents(pan);
        }
        return anyChange;
    }

    // Resolve the DoughProcessRecipe for a loose dough ItemStack
    private DoughProcessRecipe resolveProcessForStack(Level level, ItemStack stack) {
        if (stack.isEmpty()) return null;

        // 1) Preferred: process id written directly on the dough
        ResourceLocation pid = stack.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());

        // 2) Fallback: try to derive from the DoughRecipeComponent
        if (pid == null) {
            DoughRecipeComponent comp = stack.get(ModDataComponentTypes.DOUGH_RECIPE.get());
            if (comp != null) pid = resolveProcessId(comp);
        }

        if (pid == null) return null;

        // 3) Lookup recipe by id (fast path)
        Optional<DoughProcessRecipe> opt = level.getRecipeManager()
                .byKey(pid)
                .flatMap(h -> (h.value() instanceof DoughProcessRecipe dpr) ? Optional.of(dpr) : Optional.empty());

        // 4) Fallback: scan all recipes of our type and match id
        if (opt.isEmpty()) {
            final ResourceLocation pidFinal = pid; // capture for lambda
            opt = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipeTypes.DOUGH_PROCESS.get())
                    .stream()
                    .filter(h -> h.id().equals(pidFinal))
                    .findFirst()
                    .map(RecipeHolder::value);
        }

        return opt.orElse(null);
    }

    /** Prefer comp.processId()/getProcessId(); else "<base>_process". */
    private static ResourceLocation resolveProcessId(DoughRecipeComponent comp) {
        // try record accessor
        try {
            var m = DoughRecipeComponent.class.getMethod("processId");
            Object v = m.invoke(comp);
            if (v instanceof ResourceLocation rl) return rl;
            if (v instanceof String s) return ResourceLocation.parse(s);
        } catch (NoSuchMethodException ignored) {
            // try getter
            try {
                var m = DoughRecipeComponent.class.getMethod("getProcessId");
                Object v = m.invoke(comp);
                if (v instanceof ResourceLocation rl) return rl;
                if (v instanceof String s) return ResourceLocation.parse(s);
            } catch (Throwable ignored2) {}
        } catch (Throwable ignored) {}

        // fallback: derive from base ratio id
        ResourceLocation base = comp.recipeId();
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_process");
    }

    /**
     * Tick one dough piece by one tick if it’s in a PROOF step.
     * - Pre-shape PROOF: always tick.
     * - Post-shape PROOF ("final proof"): requires shaped==true, and optionally being in a pan if requirePanForFinal is true.
     * Also auto-advances PUNCHDOWN steps so chains like proof→punch→proof progress.
     *
     * @return true if the piece was modified.
     */
    private boolean tickDoughPiece(ItemStack dough, DoughProcessRecipe recipe, boolean requirePanForFinal) {
        ProofingStateComponent proof = dough.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (proof == null) return false;

        List<ProcessingStep> steps = recipe.steps();
        int idx = proof.stepIndex();
        if (idx < 0 || idx >= steps.size()) return false;

        boolean mutated = false;

        // Auto-advance any PUNCHDOWN steps immediately (they are 0-time "actions" not performed by the machine)
        // We also guard against multiple consecutive PUNCHDOWNs.
        while (idx < steps.size() && steps.get(idx).type() == StepType.PUNCHDOWN) {
            proof = new ProofingStateComponent(idx + 1,0, proof.shaped());
            dough.set(ModDataComponentTypes.PROOFING_STATE.get(), proof);
            idx = proof.stepIndex();
            mutated = true;
        }
        if (idx >= steps.size()) return mutated; // finished

        ProcessingStep current = steps.get(idx);

        // Only tick PROOF (support both single-type PROOF flow and an optional FINAL_PROOF enum if present)
        if (current.type() != StepType.PROOF && current.type() != StepType.FINAL_PROOF) {
            return mutated; // not our step; pause until external action (divide/shape) occurs
        }

        // Determine if this proof occurs after a SHAPE step in the pipeline
        boolean afterShape = steps.subList(0, idx).stream().anyMatch(s -> s.type() == StepType.SHAPE);

        // Gate final proof by shaped flag (and pan presence when required)
        if (afterShape || current.type() == StepType.FINAL_PROOF) {
            if (!proof.shaped()) return mutated;                 // needs shaping first
            if (requirePanForFinal == false) return mutated;     // final proof must be in a pan if requested
        }

        int duration = current.durationTicks(); // stored as ticks by your serializer/builder
        if (duration <= 0) {
            // Zero-duration proof; just advance
            proof = new ProofingStateComponent(idx + 1, 0, proof.shaped());
            dough.set(ModDataComponentTypes.PROOFING_STATE.get(), proof);
            return true;
        }

        int nextTicks = proof.ticksInStep() + 1;
        if (nextTicks >= duration) {
            proof = new ProofingStateComponent(idx + 1, 0, proof.shaped());
        } else {
            proof = new ProofingStateComponent(idx, nextTicks, proof.shaped());
        }
        dough.set(ModDataComponentTypes.PROOFING_STATE.get(), proof);
        return true;
    }
}
