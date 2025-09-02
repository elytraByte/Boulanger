package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.boulangermod.boulanger.component.ProofingStateComponent;
import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.ModRecipeSerializers;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.boulangermod.boulanger.screen.BakersTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class BakersTableBlockEntity extends AbstractProcessingBlockEntity implements AbstractProcessingBlock.Tickable {
    public static final int DOUGH_SLOT  = 0;
    public static final int PAN_SLOT    = 1;
    public static final int OUTPUT_SLOT = 2;

    private static final String MODID = "boulanger";

    public BakersTableBlockEntity(BlockPos pos, BlockState state) {
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
        if (level.isClientSide()) return;
        tryShape();
    }

    @SuppressWarnings("unchecked")
    private static void copyKnownDoughComponents(ItemStack source, ItemStack target) {
        for (var comp : List.of(
                ModDataComponentTypes.DOUGH_RECIPE.get(),
                ModDataComponentTypes.PROOFING_STATE.get(),
                ModDataComponentTypes.INGREDIENT_GRAMS.get(),
                ModDataComponentTypes.BAKER_PERCENTAGES.get(),
                ModDataComponentTypes.DOUGH_PROCESS_TYPE.get(),
                ModDataComponentTypes.INGREDIENT_TYPE.get()
                // intentionally NOT copying PAN_TYPE from dough
        )) {
            if (source.has(comp)) {
                target.set((net.minecraft.core.component.DataComponentType<Object>) comp, source.get(comp));
            }
        }
    }

    /** Ensure an id is namespaced; if missing, prefix with our MODID. */
    private static String normalizedPanId(String id) {
        if (id == null || id.isEmpty()) return id;
        return (id.indexOf(':') >= 0) ? id : (MODID + ":" + id);
    }

    public boolean tryShape() {
        var handler = getItemHandler(null);
        ItemStack dough  = handler.getStackInSlot(DOUGH_SLOT);
        ItemStack pan    = handler.getStackInSlot(PAN_SLOT);
        ItemStack output = handler.getStackInSlot(OUTPUT_SLOT);

        if (dough.isEmpty() || pan.isEmpty() || !output.isEmpty()) return false;

        var ds      = net.boulangermod.boulanger.component.ModDataComponentTypes.PROOFING_STATE.get();
        var pt      = net.boulangermod.boulanger.component.ModDataComponentTypes.DOUGH_PROCESS_TYPE.get();
        var panComp = net.boulangermod.boulanger.component.ModDataComponentTypes.PAN_TYPE.get();

        // Dough must have process + proofing; pan MUST declare its type (strict)
        if (!dough.has(ds) || !dough.has(pt) || !pan.has(panComp)) return false;

        // Resolve process recipe
        var recipeOpt = level.getRecipeManager()
                .getAllRecipesFor(net.boulangermod.boulanger.recipe.ModRecipeSerializers.DOUGH_PROCESS_TYPE.get()).stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(r -> r.getDoughType().equals(dough.get(pt)))
                .findFirst();
        if (recipeOpt.isEmpty()) return false;
        net.boulangermod.boulanger.recipe.DoughProcessRecipe recipe = recipeOpt.get();

        // Current step
        int idx = dough.get(ds).stepIndex();
        var steps = recipe.getSteps();
        if (idx < 0 || idx >= steps.size()) return false;

        boolean atShape  = steps.get(idx).type() == net.boulangermod.boulanger.recipe.StepType.SHAPE;
        boolean atDivide = steps.get(idx).type() == net.boulangermod.boulanger.recipe.StepType.DIVIDE;

        // Permit shaping from DIVIDE if the dough is already ~one serving (matches divider tolerance),
        // but now the serving size comes from the RatioRecipe (loaf size preferred for pan shaping).
        int shapeIdx = -1;
        if (!atShape) {
            if (!atDivide) return false;

            final double TOLERANCE_GRAMS = 2.0;
            var wc = dough.get(net.boulangermod.boulanger.component.ModDataComponentTypes.INGREDIENT_GRAMS);
            double grams = (wc != null ? wc.grams() : 0.0);

            // Find the base RatioRecipe via DoughRecipeComponent
            var base = findRatioRecipeFor(dough);
            double serving = (base != null ? base.getLoafSizeG() : 0.0);
            if (serving <= 0 && base != null) {
                // fallback to roll size if loaf size isn't defined
                serving = base.getRollSizeG();
            }

            if (!(serving > 0 && Math.abs(grams - serving) <= TOLERANCE_GRAMS)) {
                return false; // needs dividing first
            }

            // Find next SHAPE step after DIVIDE
            for (int i = idx + 1; i < steps.size(); i++) {
                if (steps.get(i).type() == net.boulangermod.boulanger.recipe.StepType.SHAPE) { shapeIdx = i; break; }
            }
            if (shapeIdx < 0) return false; // malformed process
        }

        // STRICT pan enforcement:
        // Recipe MUST declare a pan type; pan item MUST match exactly (after namespacing)
        if (recipe.getPanType() == null) return false;
        String reqNorm  = normalizedPanId(recipe.getPanType().toString());
        String presNorm = normalizedPanId(pan.get(panComp).id());
        if (!reqNorm.equals(presNorm)) return false;

        // Build shaped pan output
        ItemStack shapedPan = pan.copy();
        shapedPan.setCount(1);

        // Copy dough metadata (but NOT dough PAN_TYPE; the pan keeps its own)
        copyKnownDoughComponents(dough, shapedPan);

        // Stamp the (normalized) PAN_TYPE from the recipe
        shapedPan.set(panComp, new net.boulangermod.boulanger.component.PanTypeComponent(reqNorm));

        // Advance past SHAPE and mark shaped=true
        int nextIdxAfterShape = atShape ? (idx + 1) : (shapeIdx + 1);
        shapedPan.set(ds, new net.boulangermod.boulanger.component.ProofingStateComponent(nextIdxAfterShape, 0, true));

        // Flip the pan’s model to “full”
        var panType = net.boulangermod.boulanger.item.PanType.byId(net.minecraft.resources.ResourceLocation.tryParse(reqNorm));
        shapedPan.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(panType.getFullModelIndex()));

        // Consume inputs and output shaped pan
        dough.shrink(1);
        pan.shrink(1);
        handler.setStackInSlot(DOUGH_SLOT, dough.isEmpty() ? ItemStack.EMPTY : dough);
        handler.setStackInSlot(PAN_SLOT,  pan.isEmpty()  ? ItemStack.EMPTY : pan);
        handler.setStackInSlot(OUTPUT_SLOT, shapedPan);

        setChanged();
        return true;
    }


    // Helper: find the RatioRecipe that produced this dough (via DoughRecipeComponent.recipeId)
    @Nullable
    private net.boulangermod.boulanger.recipe.RatioRecipe findRatioRecipeFor(ItemStack dough) {
        if (level == null) return null;
        var dr = dough.get(net.boulangermod.boulanger.component.ModDataComponentTypes.DOUGH_RECIPE.get());
        if (dr == null) return null;

        return level.getRecipeManager()
                .byKey(dr.recipeId())
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(net.boulangermod.boulanger.recipe.RatioRecipe.class::isInstance)
                .map(net.boulangermod.boulanger.recipe.RatioRecipe.class::cast)
                .orElse(null);
    }


}
