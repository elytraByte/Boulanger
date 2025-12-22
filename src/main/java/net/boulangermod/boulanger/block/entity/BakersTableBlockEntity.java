package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.item.PanItem;
import net.boulangermod.boulanger.recipe.*;
import net.boulangermod.boulanger.screen.BakersTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Baker's Table — shapes dough and loads it into a pan (internal container).
 * Requirements:
 *  - Dough must have PROOFING_STATE and be at the SHAPE step (not already shaped).
 *  - Dough must also carry either a DOUGH_PROCESS_(ID/TYPE) or a DOUGH_RECIPE.
 *  - Pan is a PanItem; we size its container to capacity, insert one dough per cavity.
 *  - When full, we move the pan to OUTPUT.
 */
public class BakersTableBlockEntity extends AbstractProcessingBlockEntity implements AbstractProcessingBlock.Tickable {
    private static final Logger LOG = LogManager.getLogger();

    public static final int DOUGH_SLOT  = 0;
    public static final int PAN_SLOT    = 1;
    public static final int OUTPUT_SLOT = 2;

    public BakersTableBlockEntity(BlockPos pos, BlockState state) {
        // 3 slots: DOUGH, PAN, OUTPUT
        super(ModBlockEntities.BAKERS_TABLE.get(), pos, state, 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.boulanger.bakers_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BakersTableMenu(id, inv, this);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        // Server-side auto-shape when conditions are met
        if (level == null || level.isClientSide) return;
        try {
            var inv = this.itemHandler;
            ItemStack out   = inv.getStackInSlot(OUTPUT_SLOT);
            ItemStack dough = inv.getStackInSlot(DOUGH_SLOT);
            ItemStack pan   = inv.getStackInSlot(PAN_SLOT);

            if (out.isEmpty() && !dough.isEmpty() && !pan.isEmpty()) {
                boolean ok = this.tryShape();
                LOG.debug("[BakersTable.tick] auto tryShape() → {}", ok);
            }
        } catch (Throwable t) {
            LOG.debug("[BakersTable.tick] error in auto-shape: {}", t.toString());
        }
    }

    // ---------------------------------------------------------------------
    // Shaping action — called by menu/screen button
    // ---------------------------------------------------------------------
    public boolean tryShape() {
        if (this.level == null || this.level.isClientSide) return false;

        final var inv    = this.itemHandler;
        final ItemStack dough  = inv.getStackInSlot(DOUGH_SLOT);
        final ItemStack pan    = inv.getStackInSlot(PAN_SLOT);
        final ItemStack output = inv.getStackInSlot(OUTPUT_SLOT);

        if (dough.isEmpty() || pan.isEmpty() || !output.isEmpty()) return false;
        if (!(pan.getItem() instanceof PanItem)) return false;

        // Require proofing state and ensure we are at SHAPE step (and not already shaped)
        var ps = dough.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (ps == null) return false;
        int stepIndex = ps.stepIndex();
        if (ps.shaped()) return false;
        if (!isAtShapeStep(dough)) return false;

        // Prepare pan handler
        PanItem.ensurePanType(pan);
        PanItem.ensureContainerSized(pan);
        PanItem.PanItemHandler panHandler = new PanItem.PanItemHandler(pan);
        final int capacity = PanItem.capacityFor(pan);

        // Pan must have room
        int filledBefore = 0;
        var contBefore = pan.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if (contBefore != null) {
            int n = contBefore.getSlots();
            for (int i = 0; i < n; i++) {
                if (!contBefore.getStackInSlot(i).isEmpty()) filledBefore++;
            }
        }
        if (filledBefore >= capacity) return false;

        // Create the piece to insert and advance to the LAST PROOF step after SHAPE, if any
        ItemStack one = dough.copy();
        one.setCount(1);

        int writeIdx = stepIndex;
        var proc = resolveProcessRecipe(dough);
        if (proc != null) {
            var steps = proc.steps();
            if (steps != null && !steps.isEmpty()) {
                for (int i = Math.max(0, stepIndex + 1), n = steps.size(); i < n; i++) {
                    var t = steps.get(i).type();
                    // If you want to include FINAL_PROOF too, use: (t == StepType.PROOF || t == StepType.FINAL_PROOF)
                    if (t == StepType.PROOF) {
                        writeIdx = i; // keep updating → last PROOF after SHAPE
                    }
                }
            }
        }

        // Reset ticks and mark shaped=true
        var newPs = new ProofingStateComponent(writeIdx, 0, true);
        one.set(ModDataComponentTypes.PROOFING_STATE.get(), newPs);

        // Insert the shaped piece into the first available cavity
        ItemStack remainder = one;
        for (int slot = 0; slot < panHandler.getSlots() && !remainder.isEmpty(); slot++) {
            if (panHandler.isItemValid(slot, remainder)) {
                remainder = panHandler.insertItem(slot, remainder, false);
            }
        }
        if (!remainder.isEmpty()) return false;

        // Shrink input by 1
        ItemStack newDough = dough.copy();
        newDough.shrink(1);
        inv.setStackInSlot(DOUGH_SLOT, newDough);

        // Update model
        PanItem.syncModelToContents(pan);

        // Move pan to OUTPUT if full
        int filledAfter = 0;
        var contAfter = pan.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if (contAfter != null) {
            int n = contAfter.getSlots();
            for (int i = 0; i < n; i++) {
                if (!contAfter.getStackInSlot(i).isEmpty()) filledAfter++;
            }
        }
        if (filledAfter >= capacity) {
            inv.setStackInSlot(OUTPUT_SLOT, pan.copy());
            inv.setStackInSlot(PAN_SLOT, ItemStack.EMPTY);
        } else {
            inv.setStackInSlot(PAN_SLOT, pan);
        }

        setChanged();
        return true;
    }


    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private boolean isAtShapeStep(ItemStack dough) {
        var ps = dough.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (ps == null) { LOG.debug("[BakersTable.isAtShapeStep] no PROOFING_STATE"); return false; }

        int idx;
        try { idx = ps.stepIndex(); } catch (Throwable t) { LOG.debug("[BakersTable.isAtShapeStep] no stepIndex: {}", t.toString()); return false; }

        DoughProcessRecipe proc = resolveProcessRecipe(dough);
        if (proc == null) { LOG.debug("[BakersTable.isAtShapeStep] no process"); return false; }

        var steps = proc.steps();
        if (steps == null) { LOG.debug("[BakersTable.isAtShapeStep] steps=null"); return false; }
        if (idx < 0 || idx >= steps.size()) { LOG.debug("[BakersTable.isAtShapeStep] idx out of bounds idx={} size={}", idx, steps.size()); return false; }

        var step = steps.get(idx);
        LOG.debug("[BakersTable.isAtShapeStep] step[{}]={}", idx, step.type());
        return step.type() == StepType.SHAPE;
    }

    @Nullable
    private DoughProcessRecipe resolveProcessRecipe(ItemStack dough) {
        ResourceLocation processId = null;

        // Prefer explicit process id (newer items)
        try {
            var pid = dough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
            if (pid instanceof ResourceLocation rl) processId = rl;
        } catch (Throwable ignored) {}

        // Fallback: DOUGH_PROCESS_TYPE (older field name)
        if (processId == null) {
            try {
                var ptype = dough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
                if (ptype instanceof ResourceLocation rl) processId = rl;
            } catch (Throwable ignored2) {}
        }

        // Final fallback: use the ratio recipe id
        if (processId == null) {
            var dr = dough.get(ModDataComponentTypes.DOUGH_RECIPE.get());
            if (dr != null) processId = dr.recipeId();
        }

        LOG.debug("[BakersTable.resolveProcessRecipe] resolved candidate id={}", safeId(processId));

        // 1) Try exact key first (works if your process is registered under that id)
        if (processId != null) {
            var byKey = this.level.getRecipeManager()
                    .byKey(processId)
                    .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                    .filter(DoughProcessRecipe.class::isInstance)
                    .map(DoughProcessRecipe.class::cast);
            if (byKey.isPresent()) {
                LOG.debug("[BakersTable.resolveProcessRecipe] found by exact key: {}", byKey.get().getId());
                return byKey.get();
            }
        }

        // 2) Fallback: scan all dough-process recipes and match by dough_type
        var all = this.level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.DOUGH_PROCESS.get());
        LOG.debug("[BakersTable.resolveProcessRecipe] scanning {} process recipes for dough_type={}",
                all.size(), safeId(processId));

        for (var holder : all) {
            if (holder.value() instanceof DoughProcessRecipe r) {
                if (processId != null && processId.equals(r.getType())) {
                    LOG.debug("[BakersTable.resolveProcessRecipe] matched by dough_type: {}", r.getId());
                    return r;
                }
            }
        }

        LOG.debug("[BakersTable.resolveProcessRecipe] no process recipe found for {}", safeId(processId));
        return null;
    }


    private static String safeId(@Nullable ResourceLocation id) {
        return id == null ? "null" : id.toString();
    }

    private static String debugStack(@Nullable ItemStack s) {
        if (s == null) return "null";
        if (s.isEmpty()) return "empty";
        var key = BuiltInRegistries.ITEM.getKey(s.getItem());
        int cnt = s.getCount();
        boolean hasProof = s.has(ModDataComponentTypes.PROOFING_STATE.get());
        boolean hasRec   = s.has(ModDataComponentTypes.DOUGH_RECIPE.get());
        boolean hasProcT = false;
        boolean hasProcI = false;
        try { hasProcT = s.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get()); } catch (Throwable ignored) {}
        return key + " x" + cnt + " [proof=" + hasProof + " recipe=" + hasRec + " procType=" + hasProcT + " procId=" + hasProcI + "]";
    }
}
