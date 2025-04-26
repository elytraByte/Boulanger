package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FoodAdditiveComponent;
import net.boulangermod.boulanger.component.WeightComponent;
import net.boulangermod.boulanger.util.IngredientCategory;

public enum FoodAdditiveType {

    BUTTER             ("butter", 454f, IngredientCategory.FAT),
//    SALTED_BUTTER("butter_salted", 454f, IngredientCategory.FAT),
    EUROPEAN_BUTTER    ("european_butter", 454f, IngredientCategory.FAT),
//    SALTED_EUROPEAN_BUTTER ("european_butter_salted", 454f, IngredientCategory.FAT),
    EUROPEAN_BUTTER_BLEND ("butter", 454.0f, IngredientCategory.FAT),
//    SALTED_EUROPEAN_BUTTER_BLEND ("butter", 454.0f, IngredientCategory.FAT),
    WHOLE_MILK         ("whole_milk", 3900f, IngredientCategory.DAIRY),
    HEAVY_CREAM        ("heavy_cream", 3900f, IngredientCategory.DAIRY),
    BUTTERMILK         ("buttermilk", 3900f, IngredientCategory.DAIRY),
    SALT("salt_kosher", 1360f, IngredientCategory.SALT),
    BREWERS_YEAST      ("brewers_yeast", 226f, IngredientCategory.YEAST),
//    WILD_YEAST         ("wild_yeast", 113f, IngredientCategory.YEAST),
    SAF_RED_YEAST      ("saf_red", 454f, IngredientCategory.YEAST),
    SAF_GOLD_YEAST     ("saf_gold", 454f, IngredientCategory.YEAST),
    FLEISCHMANNS_YEAST ("fleischmanns", 454f, IngredientCategory.YEAST),
    FRESH_YEAST        ("fresh_yeast", 454f, IngredientCategory.YEAST),
    SOURDOUGH_STARTER  ("sourdough_starter", 226f, IngredientCategory.YEAST),
    RYE_SOUR_STARTER   ("rye_sour", 226f, IngredientCategory.YEAST),
    SOYBEAN_OIL        ("soybean_oil", 3500f, IngredientCategory.FAT),
    CANOLA_OIL         ("canola_oil", 3500f, IngredientCategory.FAT),
    BROWN_SUGAR        ("brown_sugar", 907f, IngredientCategory.SUGAR),
    POWDERED_SUGAR     ("powdered_sugar", 907f, IngredientCategory.SUGAR),
    MOLASSES           ("molasses", 672f, IngredientCategory.SUGAR),
//    CARAMEL_COLOR      ("caramel_color", 1000f, IngredientCategory.ADDITIVE),
    FANCY_EGG          ("fancy_egg", 50f, IngredientCategory.EGGS),
    EGG_YOLK           ("egg_yolk", 20f, IngredientCategory.EGGS),
    EGG_WHITE          ("egg_white", 30f, IngredientCategory.EGGS);


    private final String id;
    private final float weight;
    private final IngredientCategory category;

    FoodAdditiveType(String id, float weight, IngredientCategory category) {
        this.id = id;
        this.weight = weight;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public float getWeight() {
        return weight;
    }

    public IngredientCategory getCategory() {
        return category;
    }

    /**
     * Converts this enum constant into a Food Additive data component.
     */
    public FoodAdditiveComponent toFoodAdditiveComponent() {
        // You may choose to include the category in the component too,
        // or simply rely on this enum's mapping later in your mixing logic.
        return new FoodAdditiveComponent(id, new WeightComponent(weight));
    }

    public static FoodAdditiveType fromId(String id) {
        for (FoodAdditiveType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return FoodAdditiveType.BUTTER; // Default value
    }
}