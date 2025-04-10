package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FoodAdditiveComponent;

public enum FoodAdditiveType {

    BUTTER ("butter", 454f),
    UNSALTED_BUTTER ("butter_salted", 454f),
    EUROPEAN_BUTTER ("european_butter", 454f),
    SALTED_EUROPEAN_BUTTER ("european_butter_salted", 454f),
    EUROPEAN_BUTTER_BLEND ("butter", 454.0f),
    SALTED_EUROPEAN_BUTTER_BLEND ("butter", 454.0f),
    WHOLE_MILK ("whole_milk", 3900f),
    HEAVY_CREAM ("heavy_cream", 3900f),
    BUTTERMILK ("buttermilk", 3900f),
    SALT ("salt", 1360f),
    BREWERS_YEAST ("brewers_yeast", 226f),
    WILD_YEAST ("wild_yeast", 113f),
    SAF_RED_YEAST ("saf_red", 454f),
    SAF_GOLD_YEAST ("saf_gold", 454f),
    FLEISCHMANNS_YEAST("fleischmanns", 454f),
    FRESH_YEAST ("fresh_yeast", 454f),
    SOURDOUGH_STARTER ("sourdough_starter", 226f),
    RYE_SOUR_STARTER ("rye_sour", 226f),
    SOYBEAN_OIL ("soybean_oil", 3500f),
    CANOLA_OIL ("canola_oil", 3500f),
    BROWN_SUGAR ("brown_sugar", 907f),
    POWDERED_SUGAR ("powdered_sugar", 907f),
    MOLASSES ("molasses", 672f),
    CARAMEL_COLOR ("caramel_color", 1000f),
    FANCY_EGG ("fancy_egg", 50f),
    EGG_YOLK ("egg_yolk", 20f),
    EGG_WHITE ("egg_white", 30f);

    private final String id;
    private final float weight;

    FoodAdditiveType(String id, float weight) {
        this.id = id;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public float getWeight() {
        return weight;
    }

    /**
     * Converts this enum constant into a Food Additive data component.
     * Adjust the constructor and parameters for FoodAdditiveComponent according to your project's requirements.
     */
    public FoodAdditiveComponent toFoodAdditiveComponent() {
        return new FoodAdditiveComponent(id, weight);
    }

    /**
     * Looks up a FoodAdditiveType based on its identifier.
     * Returns a default type if no match is found.
     */
    public static FoodAdditiveType fromId(String id) {
        for (FoodAdditiveType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return FoodAdditiveType.BUTTER; // Default value choice
    }
}
