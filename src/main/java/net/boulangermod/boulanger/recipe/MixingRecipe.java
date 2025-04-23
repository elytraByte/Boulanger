package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.recipe.RatioRecipe.IngredientRequirement;
import net.boulangermod.boulanger.util.IngredientStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A single mixing-bowl recipe expressed in baker’s percentages,
 * with optional per-item requirements.
 */
public final class MixingRecipe {
    private final ResourceLocation id;
    private final Map<IngredientCategory, Double> targetPercentages;
    private final Item resultItem;
    private final Set<String> allowedFlourTypeIds;
    private final List<IngredientRequirement> itemRequirements;

    /**
     * Full constructor, including optional per-item requirements.
     */
    public MixingRecipe(ResourceLocation id,
                        Map<IngredientCategory, Double> targets,
                        Item resultItem,
                        Set<String> allowedFlourTypeIds,
                        List<IngredientRequirement> itemRequirements) {
        this.id = id;
        this.targetPercentages = targets;
        this.resultItem = resultItem;
        this.allowedFlourTypeIds = allowedFlourTypeIds;
        this.itemRequirements = List.copyOf(itemRequirements);
    }

    public static class IngredientRequirement {
        private final ResourceLocation itemId;
        private final IngredientCategory category;
        private final double percentage;

        public IngredientRequirement(ResourceLocation itemId,
                                     IngredientCategory category,
                                     double percentage) {
            this.itemId     = itemId;
            this.category   = category;
            this.percentage = percentage;
        }

        /** The specific item this requirement applies to (e.g. whole_wheat_flour). */
        public ResourceLocation getItemId() {
            return itemId;
        }

        /** The category for this requirement (e.g. FLOUR). */
        public IngredientCategory getCategory() {
            return category;
        }

        /** The baker’s-percentage for this item (e.g. 75.0). */
        public double getPercentage() {
            return percentage;
        }

        @Override
        public String toString() {
            return "IngredientRequirement{" +
                    "item=" + itemId +
                    ", category=" + category +
                    ", pct=" + percentage +
                    '}';
        }
    }

    /**
     * Backwards-compatible constructor with no per-item requirements.
     */
    public MixingRecipe(ResourceLocation id,
                        Map<IngredientCategory, Double> targets,
                        Item resultItem,
                        Set<String> allowedFlourTypeIds) {
        this(id, targets, resultItem, allowedFlourTypeIds, List.of());
    }

    public ResourceLocation getId() {
        return id;
    }

    /** The target baker’s percentages by category (e.g. FLOUR=100, YEAST=2). */
    public Map<IngredientCategory, Double> targetPercentages() {
        return targetPercentages;
    }

    public Item getResultItem() {
        return resultItem;
    }

    public Set<String> getAllowedFlourTypeIds() {
        return allowedFlourTypeIds;
    }

    /**
     * Any per-item baker’s percentage requirements (e.g. 75% whole wheat, 25% bread flour).
     */
    public List<IngredientRequirement> getItemRequirements() {
        return itemRequirements;
    }

    @Override
    public String toString() {
        return "MixingRecipe{" +
                "id=" + id +
                ", targets=" + targetPercentages +
                ", resultItem=" + resultItem +
                ", allowedFlours=" + allowedFlourTypeIds +
                ", itemReqs=" + itemRequirements +
                '}';
    }

    /**
     * Returns true if this recipe matches the given ingredient stacks
     * when compared against the provided actual baker’s percentages.
     */
    public boolean matches(List<IngredientStack> ingredientList,
                           Map<IngredientCategory, Double> actualPct) {
        double tol = 2.0; // percent tolerance

        // 1) check each target percentage
        for (var entry : targetPercentages.entrySet()) {
            double got = actualPct.getOrDefault(entry.getKey(), 0.0);
            if (Math.abs(got - entry.getValue()) > tol) {
                return false;
            }
        }

        // 2) enforce flour‐type whitelist (if present)
        if (!allowedFlourTypeIds.isEmpty()) {
            for (IngredientStack st : ingredientList) {
                if (st.getCategory() == IngredientCategory.FLOUR) {
                    String id = st.getFlourType() != null
                            ? st.getFlourType().getId()
                            : "<none>";
                    if (!allowedFlourTypeIds.contains(id)) {
                        return false;
                    }
                }
            }
        }

        // 3) enforce any per-item requirements
        for (IngredientRequirement req : itemRequirements) {
            double got = actualPct.getOrDefault(req.getCategory(), 0.0);
            if (Math.abs(got - req.getPercentage()) > tol) {
                return false;
            }
        }

        // all checks passed
        return true;
    }
}
