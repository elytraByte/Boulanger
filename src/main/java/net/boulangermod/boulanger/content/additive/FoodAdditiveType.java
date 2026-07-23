package net.boulangermod.boulanger.content.additive;

import net.boulangermod.boulanger.component.value.FoodAdditiveComponent;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum FoodAdditiveType {

    BUTTER             ("butter", 454000L, IngredientCategory.FAT),
    BUTTER_SALTED("butter_salted", 454000L, IngredientCategory.FAT),
    EUROPEAN_BUTTER    ("european_butter", 454000L, IngredientCategory.FAT),
    EUROPEAN_BUTTER_SALTED("european_butter_salted", 454000L, IngredientCategory.FAT),
    EUROPEAN_BUTTER_BLEND ("european_butter_blend", 454000L, IngredientCategory.FAT),
    EUROPEAN_BUTTER_BLEND_SALTED ("european_butter_blend_salted", 454000L, IngredientCategory.FAT),
    LARD ("lard", 454000L, IngredientCategory.FAT),
    MARGARINE ("margarine", 454000L, IngredientCategory.FAT),
    WHOLE_MILK         ("whole_milk", 3900000L, IngredientCategory.DAIRY),
    HEAVY_CREAM        ("heavy_cream", 3900000L, IngredientCategory.DAIRY),
    BUTTERMILK         ("buttermilk", 3900000L, IngredientCategory.DAIRY),
    DRY_WHOLE_MILK_POWDER         ("dry_whole_milk_powder", 113000L, IngredientCategory.DAIRY),
    DRY_BUTTERMILK_POWDER         ("dry_buttermilk_powder", 113000L, IngredientCategory.DAIRY),
    KOSHER_SALT        ("kosher_salt", 1360000L, IngredientCategory.SALT),
    BREWERS_YEAST      ("brewers_yeast", 226000L, IngredientCategory.YEAST),
    //WILD_YEAST         ("wild_yeast", 113f, IngredientCategory.YEAST),
    SAF_RED_YEAST      ("saf_red", 454000L, IngredientCategory.YEAST),
    SAF_GOLD_YEAST     ("saf_gold", 454000L, IngredientCategory.YEAST),
    FLEISCHMANNS_YEAST ("fleischmanns_yeast", 454000L, IngredientCategory.YEAST),
    FRESH_YEAST        ("fresh_yeast", 454000L, IngredientCategory.YEAST),
    SOURDOUGH_STARTER  ("sourdough_starter", 226000L, IngredientCategory.YEAST),
    RYE_SOUR_STARTER   ("rye_sour_starter", 226000L, IngredientCategory.YEAST),
    SOYBEAN_OIL        ("soybean_oil", 3500000L, IngredientCategory.FAT),
    CANOLA_OIL         ("canola_oil", 3500000L, IngredientCategory.FAT),
    BROWN_SUGAR        ("brown_sugar", 907000L, IngredientCategory.SUGAR),
    POWDERED_SUGAR     ("powdered_sugar", 907000L, IngredientCategory.SUGAR),
    MOLASSES           ("molasses", 672000L, IngredientCategory.SUGAR),
    //CARAMEL_COLOR      ("caramel_color", 1000f, IngredientCategory.ADDITIVE),
    EGG_YOLK           ("egg_yolk", 20000L, IngredientCategory.EGGS),
    EGG_WHITE          ("egg_white", 30000L, IngredientCategory.EGGS);

    private final String id;
    private final long unitMg;
    private final IngredientCategory category;

    FoodAdditiveType(String id, long unitGrams, IngredientCategory category) {
        this.id = id;
        this.unitMg = (int) Math.round(unitGrams * 1000.0);
        this.category = category;
    }

    public String id() { return id; }
    public long unitMg() { return unitMg; }
    public double unitGrams() { return unitMg / 1000.0; }
    public IngredientCategory category() { return category; }

    public FoodAdditiveComponent toComponent() {
        return new FoodAdditiveComponent(id);
    }

    private static final Map<String, FoodAdditiveType> BY_ID =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(FoodAdditiveType::id, type -> type));

    public static Optional<FoodAdditiveType> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
