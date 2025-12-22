package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeTypes;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

/**
 * PanItem:
 *  - Stores per-stack family (PAN_TYPE) but can also infer from CMD or item default.
 *  - Container-backed cavities via DataComponents.CONTAINER (sized to capacity).
 *  - Model flips (empty/full) driven strictly by contents → stable textures.
 */
public class PanItem extends Item {

    private static final Logger LOG = LogManager.getLogger();

    /**
     * The baked-in default family for this concrete item (used as a last resort).
     */
    public final PanType panType;

    public PanItem(Properties props, PanType panType) {
        super(props);
        this.panType = panType;
    }

    /* -------------------------------------------------------------------------
       Defaults & lifecycle
       ------------------------------------------------------------------------- */

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        // Write the family so capacity is unambiguous from frame 0
        stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(this.panType.getId()));
        // Size container to capacity (empty)
        ensureContainerSized(stack);
        // Force EMPTY model for fresh stacks (no flicker)
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(this.panType.getEmptyModelIndex()));
        return stack;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        // Some craft paths bypass getDefaultInstance → normalize here
        ensurePanType(stack);
        ensureContainerSized(stack);
        syncModelToContents(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // Idempotent: flips model if contents changed elsewhere
        syncModelToContents(stack);
    }

    /* -------------------------------------------------------------------------
       Tooltip (null-safe)
       ------------------------------------------------------------------------- */

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PanType t = typeOf(stack);
        int cap = capacityFor(stack);
        int filled = 0;
        ItemContainerContents cont = stack.get(DataComponents.CONTAINER);
        if (cont != null) {
            int n = Math.min(cap, cont.getSlots());
            for (int i = 0; i < n; i++) if (!cont.getStackInSlot(i).isEmpty()) filled++;
        }
        String label = (t != null ? t.getId() : "unknown");
        tooltip.add(Component.literal("Pan: " + label));
        tooltip.add(Component.literal("Capacity: " + cap + " • Filled: " + filled));

        // — Details for ONE dough piece (first non-empty), only when user holds Shift —
        boolean show = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        if (!show) {
            tooltip.add(Component.literal("§7(hold §fShift§7 for details)"));
            return;
        }

        ItemStack piece = firstDoughPiece(stack);
        if (piece.isEmpty()) {
            tooltip.add(Component.literal("§7(no dough pieces in pan)"));
            return;
        }

        // Pull components off the dough piece
        var DS = ModDataComponentTypes.PROOFING_STATE.get();
        var PT = ModDataComponentTypes.DOUGH_PROCESS_TYPE.get();
        var DR = ModDataComponentTypes.DOUGH_RECIPE.get();

        var proof = piece.get(DS);
        var procComp = piece.get(PT);
        var recipeComp = piece.get(DR);

        // Recipe id
        String recipePath = "unknown";
        try {
            if (recipeComp != null) recipePath = recipeComp.recipeId().getPath();
        } catch (Throwable ignored) {}

        tooltip.add(Component.literal("§fDough: §7" + recipePath));

        // Step + shaped
        int idx = -1, ticks = 0, stepCount = -1;
        String stepName = "?";
        boolean shaped = false;

        if (proof != null) {
            try { idx = proof.stepIndex(); } catch (Throwable ignored) {}
            try { ticks = proof.ticksInStep(); } catch (Throwable ignored) {}
            try { shaped = proof.shaped(); } catch (Throwable ignored) {}
        }

        // Resolve process client-side to name the current step
        DoughProcessRecipe proc = tryResolveProcessClient(piece);
        if (proc != null) {
            List<ProcessingStep> steps = proc.steps();
            if (steps != null) {
                stepCount = steps.size();
                if (idx >= 0 && idx < steps.size()) {
                    var st = steps.get(idx);
                    try { stepName = st.type().name(); } catch (Throwable ignored) {}
                }
            }
        }

        String stepLine = "§fStep: §7" + stepName + (stepCount > 0 ? " (" + (idx + 1) + "/" + stepCount + ")" : "");
        tooltip.add(Component.literal(stepLine));
        tooltip.add(Component.literal("§fShaped: §7" + shaped));

        // If currently a PROOF step with duration, show % progress
        Integer duration = currentStepDurationTicks(proc, idx);
        if (duration != null && duration > 0 && "PROOF".equalsIgnoreCase(stepName)) {
            int pct = Math.min(100, Math.max(0, Math.round((ticks * 100f) / duration)));
            tooltip.add(Component.literal("§fProof: §a" + pct + "%"));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        ItemStack piece = firstDoughPiece(stack);
        if (piece.isEmpty()) return false;

        var proof = piece.get(ModDataComponentTypes.PROOFING_STATE.get());
        if (proof == null) return false;

        int idx;
        try { idx = proof.stepIndex(); } catch (Throwable t) { return false; }

        DoughProcessRecipe proc = tryResolveProcessClient(piece);
        if (proc == null) return false;

        // Only show a bar for PROOF steps that have a duration
        var dur = currentStepDurationTicks(proc, idx);
        if (dur == null || dur <= 0) return false;

        var steps = proc.steps();
        if (steps == null || idx < 0 || idx >= steps.size()) return false;
        var st = steps.get(idx);
        return st.type() == net.boulangermod.boulanger.recipe.StepType.PROOF;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        ItemStack piece = firstDoughPiece(stack);
        var proof = (piece.isEmpty() ? null : piece.get(ModDataComponentTypes.PROOFING_STATE.get()));
        if (proof == null) return 0;

        int idx, ticks;
        try { idx = proof.stepIndex(); } catch (Throwable t) { return 0; }
        try { ticks = proof.ticksInStep(); } catch (Throwable t) { ticks = 0; }

        DoughProcessRecipe proc = tryResolveProcessClient(piece);
        Integer dur = (proc == null ? null : currentStepDurationTicks(proc, idx));
        if (dur == null || dur <= 0) return 0;

        float f = Math.min(1f, Math.max(0f, ticks / (float) dur));
        return Math.round(13 * f); // same width scale vanilla uses
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // red -> yellow -> green as progress advances
        ItemStack piece = firstDoughPiece(stack);
        var proof = (piece.isEmpty() ? null : piece.get(ModDataComponentTypes.PROOFING_STATE.get()));
        if (proof == null) return 0x00FF00;

        int idx, ticks;
        try { idx = proof.stepIndex(); } catch (Throwable t) { return 0x00FF00; }
        try { ticks = proof.ticksInStep(); } catch (Throwable t) { ticks = 0; }

        DoughProcessRecipe proc = tryResolveProcessClient(piece);
        Integer dur = (proc == null ? null : currentStepDurationTicks(proc, idx));
        if (dur == null || dur <= 0) return 0x00FF00;

        float f = Math.min(1f, Math.max(0f, ticks / (float) dur));
        // HSV hue 0.0 (red) -> 0.33 (green)
        return net.minecraft.util.Mth.hsvToRgb(0.33f * f, 1.0f, 1.0f);
    }

    private static ItemStack firstDoughPiece(ItemStack pan) {
        ItemContainerContents cont = pan.get(DataComponents.CONTAINER);
        if (cont == null) return ItemStack.EMPTY;

        // Only iterate over actually stored entries; 0 means no slots to read.
        int n = cont.getSlots();
        if (n <= 0) return ItemStack.EMPTY;

        for (int i = 0; i < n; i++) {
            ItemStack s = cont.getStackInSlot(i);
            if (!s.isEmpty()) return s;
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasAnyContents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ItemContainerContents cont = stack.get(DataComponents.CONTAINER);
        if (cont == null) return false;

        int cap = Math.max(1, capacityFor(stack));
        int n = Math.min(cap, cont.getSlots());
        for (int i = 0; i < n; i++) {
            if (!cont.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        // If there’s any dough inside, disallow stacking.
        // Otherwise fall back to the configured default (e.g., stacksTo(16)).
        return hasAnyContents(stack) ? 1 : super.getMaxStackSize(stack);
    }


    /** Resolve the dough process on the CLIENT (needed to read step durations in tooltips/bars). */
    @Nullable
    private static DoughProcessRecipe tryResolveProcessClient(ItemStack dough) {
        // Prefer explicit process/type id, else fall back to the ratio recipe id
        ResourceLocation processId = null;
        try {
            var proc = dough.get(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get());
            if (proc instanceof ResourceLocation rl) processId = rl;
            else if (proc != null) {
                try { var m = proc.getClass().getMethod("id"); Object s = m.invoke(proc); if (s != null) processId = ResourceLocation.tryParse(s.toString()); }
                catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        if (processId == null) {
            var dr = dough.get(ModDataComponentTypes.DOUGH_RECIPE.get());
            if (dr != null) {
                try { processId = dr.recipeId(); } catch (Throwable ignored) {}
            }
        }

        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.level == null) return null;

        // 1) exact key
        var byKey = mc.level.getRecipeManager()
                .byKey(processId)
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(net.boulangermod.boulanger.recipe.DoughProcessRecipe.class::isInstance)
                .map(net.boulangermod.boulanger.recipe.DoughProcessRecipe.class::cast);
        if (byKey.isPresent()) return byKey.get();

        // 2) by dough_type (back-compat: processes keyed like dough_process/baguette)
        var all = mc.level.getRecipeManager().getAllRecipesFor(
                ModRecipeTypes.DOUGH_PROCESS.get()
        );
        for (var h : all) {
            if (h.value() instanceof net.boulangermod.boulanger.recipe.DoughProcessRecipe r) {
                if (processId != null && processId.equals(r.getType())) return r;
            }
        }
        return null;
    }

    /** Duration (ticks) of current step, or null if unavailable. */
    @Nullable
    private static Integer currentStepDurationTicks(@Nullable DoughProcessRecipe proc, int stepIndex) {
        if (proc == null) return null;
        List<ProcessingStep> steps = proc.steps();
        if (steps == null || stepIndex < 0 || stepIndex >= steps.size()) return null;
        try {
            int d = steps.get(stepIndex).durationTicks();
            return (d > 0 ? d : null);
        } catch (Throwable t) {
            return null;
        }
    }


    /**
     * Resolve the pan family: PAN_TYPE → CMD → item default.
     */
    public static @Nullable PanType typeOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        // 1) Authoritative per-stack component
        PanTypeComponent comp = stack.get(ModDataComponentTypes.PAN_TYPE.get());
        ResourceLocation rl = ResourceLocation.tryParse(comp.id());
        if (rl != null) {
            PanType t = PanType.byId(rl);
            if (t != null) return t;
        }


        // 2) Infer from current custom model data
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            int v = cmd.value();
            for (PanType t : PanType.values()) {
                if (t.getEmptyModelIndex() == v || t.getFullModelIndex() == v) return t;
            }
        }

        // 3) Last resort: this concrete item’s baked-in default
        if (stack.getItem() instanceof PanItem pi) return pi.panType;

        return null;
    }

    /**
     * Capacity for this stack; safe fallback to LOAF if unknown.
     */
    public static int capacityFor(ItemStack stack) {
        PanType t = typeOf(stack);
        return (t != null ? t.getCapacity() : PanType.LOAF.getCapacity());
    }

    /**
     * Ensure PAN_TYPE exists on the stack (nice for data-driven paths).
     */
    public static void ensurePanType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (stack.has(ModDataComponentTypes.PAN_TYPE.get())) return;
        PanType t = typeOf(stack);
        if (t != null) {
            stack.set(ModDataComponentTypes.PAN_TYPE.get(), new PanTypeComponent(t.getId()));
        }
    }

    /**
     * Flip CMD (empty/full) based on actual contents; no PAN_TYPE requirement.
     */
    public static void syncModelToContents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        PanType t = typeOf(stack);
        if (t == null) return; // unknown item; don't touch

        int cap = Math.max(1, capacityFor(stack));
        ItemContainerContents cont = stack.get(DataComponents.CONTAINER);
        int filled = 0;
        if (cont != null) {
            int n = Math.min(cap, cont.getSlots());
            for (int i = 0; i < n; i++) if (!cont.getStackInSlot(i).isEmpty()) filled++;
        }

        int model = (filled > 0) ? t.getFullModelIndex() : t.getEmptyModelIndex();
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(model));
    }

    /* -------------------------------------------------------------------------
       Container helpers (DataComponents.CONTAINER)
       ------------------------------------------------------------------------- */

    /**
     * Guarantee CONTAINER exists and is exactly the pan's capacity; preserve items.
     */
    public static void ensureContainerSized(ItemStack stack) {
        int cap = Math.max(1, capacityFor(stack));
        ItemContainerContents cont = stack.get(DataComponents.CONTAINER);

        // Already correct size
        if (cont != null && cont.getSlots() == cap) return;

        NonNullList<ItemStack> list = NonNullList.withSize(cap, ItemStack.EMPTY);
        if (cont != null) {
            int n = Math.min(cap, cont.getSlots());
            for (int i = 0; i < n; i++) {
                list.set(i, cont.getStackInSlot(i));
            }
        }
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }

    /* -------------------------------------------------------------------------
       NeoForge item-handler capability for pans (used by oven/table)
       ------------------------------------------------------------------------- */

    public static class PanItemHandler implements IItemHandler {
        private final ItemStack panStack;

        public PanItemHandler(ItemStack panStack) {
            this.panStack = panStack;
            // Normalize family & container
            PanItem.ensurePanType(this.panStack);
            PanItem.ensureContainerSized(this.panStack);
        }

        @Override
        public int getSlots() {
            return Math.max(1, PanItem.capacityFor(panStack));
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemContainerContents c = panStack.get(DataComponents.CONTAINER);
            if (c == null || slot < 0 || slot >= c.getSlots()) return ItemStack.EMPTY;
            ItemStack s = c.getStackInSlot(slot);
            return s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }

        /**
         * One dough per cavity.
         */
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        /**
         * Match Bakers Table: dough-like = proof + (process || recipe).
         */
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;

            boolean hasProof = stack.has(ModDataComponentTypes.PROOFING_STATE.get());
            boolean hasRecipe = stack.has(ModDataComponentTypes.DOUGH_RECIPE.get());

            boolean hasProcess = false;
            try { hasProcess = stack.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get()); }
            catch (Throwable ignored) {
                try { hasProcess = stack.has(ModDataComponentTypes.DOUGH_PROCESS_TYPE.get()); }
                catch (Throwable ignored2) {}
            }

            // Require proofing metadata AND (recipe or process) to be treated as real dough
            return hasProof && (hasRecipe || hasProcess);
        }

        /**
         * Insert one dough item into the given slot; returns remainder.
         */
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) {
                if (LOG.isDebugEnabled()) LOG.debug("[PanItemHandler.insert] abort: incoming stack empty");
                return ItemStack.EMPTY;
            }
            if (!isItemValid(slot, stack)) {
                if (LOG.isDebugEnabled()) LOG.debug("[PanItemHandler.insert] abort: isItemValid=false for slot={} stack={}", slot, stack);
                return stack;
            }

            // Use declared capacity, not c.getSlots()
            final int cap = Math.max(1, PanItem.capacityFor(panStack));
            if (slot < 0 || slot >= cap) {
                if (LOG.isDebugEnabled()) LOG.debug("[PanItemHandler.insert] abort: slot OOB slot={} cap={}", slot, cap);
                return stack;
            }

            // Load current contents (may be null or sparse) and expand to 'cap'
            ItemContainerContents c = panStack.get(DataComponents.CONTAINER);
            NonNullList<ItemStack> list = NonNullList.withSize(cap, ItemStack.EMPTY);
            int present = 0;
            if (c != null) {
                present = Math.min(cap, c.getSlots()); // number of stored (non-empty) entries
                for (int i = 0; i < present; i++) {
                    list.set(i, c.getStackInSlot(i));
                }
            }

            ItemStack inSlot = (slot < present) ? list.get(slot) : ItemStack.EMPTY;
            if (LOG.isDebugEnabled()) {
                LOG.debug("[PanItemHandler.insert] pre: slot={} cap={} present={} inSlotEmpty={} inSlot={}",
                        slot, cap, present, inSlot.isEmpty(), inSlot);
            }
            if (!inSlot.isEmpty()) {
                if (LOG.isDebugEnabled()) LOG.debug("[PanItemHandler.insert] abort: slot {} already occupied", slot);
                return stack; // no stacking
            }

            // Place exactly one
            ItemStack placed = stack.copy();
            placed.setCount(1);
            if (!simulate) {
                list.set(slot, placed);
                // fromItems will compact trailing empties, which is fine — we always rebuild to 'cap' when editing
                panStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
                PanItem.syncModelToContents(panStack);
                if (LOG.isDebugEnabled()) LOG.debug("[PanItemHandler.insert] wrote slot={} nowModelSynced", slot);
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            if (LOG.isDebugEnabled()) {
                LOG.debug("[PanItemHandler.insert] success: returning remainder={} (isEmpty={})", remainder, remainder.isEmpty());
            }
            return remainder;

        }

        /**
         * Remove up to `amount` items from `slot` and return them.
         */
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) return ItemStack.EMPTY;

            PanItem.ensureContainerSized(panStack);
            ItemContainerContents c = panStack.get(DataComponents.CONTAINER);
            if (c == null || slot < 0 || slot >= c.getSlots()) return ItemStack.EMPTY;

            ItemStack in = c.getStackInSlot(slot);
            if (in.isEmpty()) return ItemStack.EMPTY;

            int toExtract = Math.min(amount, in.getCount());
            ItemStack extracted = in.copy();
            extracted.setCount(toExtract);

            if (!simulate) {
                ItemStack remainder = in.copy();
                remainder.shrink(toExtract);

                int cap = Math.max(1, c.getSlots());
                NonNullList<ItemStack> list = NonNullList.withSize(cap, ItemStack.EMPTY);
                for (int i = 0; i < cap; i++) list.set(i, c.getStackInSlot(i));
                list.set(slot, remainder.isEmpty() ? ItemStack.EMPTY : remainder);
                panStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));

                PanItem.syncModelToContents(panStack);
            }
            return extracted;
        }
    }

}