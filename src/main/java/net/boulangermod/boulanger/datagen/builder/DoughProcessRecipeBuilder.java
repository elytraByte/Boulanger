package net.boulangermod.boulanger.datagen.builder;

import net.boulangermod.boulanger.item.PanType;
import net.boulangermod.boulanger.item.PortionKind;
import net.boulangermod.boulanger.recipe.DoughProcessRecipe;
import net.boulangermod.boulanger.recipe.PanServing;
import net.boulangermod.boulanger.recipe.ProcessingStep;
import net.boulangermod.boulanger.recipe.StepType;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DoughProcessRecipeBuilder {
    private final ResourceLocation id;
    private final List<ProcessingStep> steps = new ArrayList<>();
    private final Map<PortionKind, PanServing> serving = new EnumMap<>(PortionKind.class);

    public DoughProcessRecipeBuilder(ResourceLocation id) {
        this.id = id;
    }

    // ===== Step helpers (minutes-only) =====
    /** PROOF for the given real-world minutes. Fractions are rounded to nearest minute. */
    public DoughProcessRecipeBuilder proof(double minutes) {
        int mins = Math.max(0, (int)Math.round(minutes));
        this.steps.add(new ProcessingStep(StepType.PROOF, mins));
        return this;
    }

    /** PUNCHDOWN (no duration). */
    public DoughProcessRecipeBuilder punchdown() {
        this.steps.add(new ProcessingStep(StepType.PUNCHDOWN, 0));
        return this;
    }

    /** DIVIDE (no duration). */
    public DoughProcessRecipeBuilder divide() {
        this.steps.add(new ProcessingStep(StepType.DIVIDE, 0));
        return this;
    }

    /** SHAPE (no duration). */
    public DoughProcessRecipeBuilder shape() {
        this.steps.add(new ProcessingStep(StepType.SHAPE, 0));
        return this;
    }

    // ===== Per-portion pan rules =====
    public DoughProcessRecipeBuilder serve(PortionKind kind, int servingWeightG,
                                           PanType panType, int perPanCapacity) {
        this.serving.put(kind, new PanServing(servingWeightG, panType, perPanCapacity));
        return this;
    }

    // ===== Emit =====
    public void save(RecipeOutput out) {
        DoughProcessRecipe recipe = new DoughProcessRecipe(
                id,
                List.copyOf(this.steps),
                Map.copyOf(this.serving)
        );
        out.accept(id, recipe, null);
    }
}
