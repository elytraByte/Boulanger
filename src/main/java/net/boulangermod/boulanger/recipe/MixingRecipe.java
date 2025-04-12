package net.boulangermod.boulanger.recipe;

import net.boulangermod.boulanger.util.IngredientCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.Set;

/**
 * A single mixing‑bowl recipe expressed in baker’s percentages.
 *
 * If {@code allowedFlourTypeIds} is empty the recipe accepts any flour.
 * Otherwise every flour bowl in the mix must carry a {@code FlourType}
 * whose {@code id} is contained in that set.
 */
public final class MixingRecipe {

    private final ResourceLocation id;
    private final Map<IngredientCategory, Double> targetPercentages;
    private final Item               resultItem;
    private final Set<String>        allowedFlourTypeIds;   // ← new field

    public MixingRecipe(ResourceLocation id,
                        Map<IngredientCategory, Double> targetPercentages,
                        Item resultItem,
                        Set<String> allowedFlourTypeIds) {
        this.id                   = id;
        this.targetPercentages    = Map.copyOf(targetPercentages);
        this.resultItem           = resultItem;
        this.allowedFlourTypeIds  = Set.copyOf(allowedFlourTypeIds);
    }

    /* getters */
    public ResourceLocation getId()                         { return id; }
    public Map<IngredientCategory, Double> targetPercentages() { return targetPercentages; }
    public Item resultItem()                                { return resultItem; }
    public Set<String> getAllowedFlourTypeIds()             { return allowedFlourTypeIds; }

    @Override
    public String toString() {
        return "MixingRecipe{" +
                "id=" + id +
                ", targetPercentages=" + targetPercentages +
                ", resultItem=" + resultItem +
                ", allowedFlourTypeIds=" + allowedFlourTypeIds +
                '}';
    }
}
