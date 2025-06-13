package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.item.FiftyPoundBagItem;

public enum FiftyPoundBagType {
    ALL_PURPOSE("all_purpose_flour", 101, 0.45f, 10.5f),
    BREAD("bread_flour", 102, 0.55f, 12.0f),
    HIGH_GLUTEN("high_gluten_flour", 103, 0.50f, 14.0f),
    RYE("rye_flour", 104, 1.30f, 8.0f),
    SEMOLINA("semolina_flour", 105, 0.90f, 11.0f),
    VWG("vital_wheat_gluten", 106, 0.80f, 75.0f),
    WHOLE_WHEAT("whole_wheat_flour", 107, 1.70f, 13.0f);

    private final String id; // full flour id, not shorthand
    private final int modelIndex;
    private final float ash;
    private final float protein;

    FiftyPoundBagType(String id, int modelIndex, float ash, float protein) {
        this.id = id;
        this.modelIndex = modelIndex;
        this.ash = ash;
        this.protein = protein;
    }

    public String getId() {
        return id;
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public FlourType toFlourType() {
        // Removed namespace to align with FlourItemType
        return new FlourType(id, ash, protein, modelIndex, FiftyPoundBagItem.MAX_GRAMS);
    }
}
