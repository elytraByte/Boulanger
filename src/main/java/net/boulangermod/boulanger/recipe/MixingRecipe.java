package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.recipe.RatioRecipe.IngredientRequirement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A single mixing‑bowl recipe expressed in baker’s percentages,
 * with optional per‑item requirements.
 */
public final class MixingRecipe {
    private final ResourceLocation id;
    private final Map<IngredientCategory, Double> targetPercentages;
    private final Item resultItem;
    private final Set<String> allowedFlourTypeIds;
    private final List<IngredientRequirement> itemRequirements;

    /**
     * Full constructor, including optional per‑item requirements.
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

    /**
     * Backwards‑compatible constructor with no per‑item requirements.
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
     * Returns any per‑item baker’s percentage requirements (may be empty).
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
}
