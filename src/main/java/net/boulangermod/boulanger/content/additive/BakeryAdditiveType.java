package net.boulangermod.boulanger.content.additive;

import net.boulangermod.boulanger.component.value.BakeryAdditiveComponent;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum BakeryAdditiveType {
    ASCORBIC_ACID            ("ascorbic_acid",             454.0, IngredientCategory.ADDITIVE),
    CALCIUM_PROPIONATE       ("calcium_propionate",        454.0, IngredientCategory.ADDITIVE),
    DIASTATIC_MALT_POWDER    ("diastatic_malt_powder",     454.0, IngredientCategory.ADDITIVE),
    NONDIASTATIC_MALT_POWDER ("nondiastatic_malt_powder",  454.0, IngredientCategory.ADDITIVE),
    L_CYSTEINE               ("l_cysteine",                113.0, IngredientCategory.ADDITIVE),
    S_500_RED                ("s_500_red",                 22680, IngredientCategory.ADDITIVE),
    IM_PROVE_200             ("im_prove_200",              20412, IngredientCategory.ADDITIVE),
    ADVANTAGE_500_CL         ("advantage_500_cl",          10000, IngredientCategory.ADDITIVE);

    private final String id;
    private final int unitMg;
    private final IngredientCategory category;

    BakeryAdditiveType(String id, double unitGrams, IngredientCategory category) {
        this.id = id;
        this.unitMg = (int) Math.round(unitGrams * 1000.0);
        this.category = category;
    }

    public String id() { return id; }
    public int unitMg() { return unitMg; }
    public double unitGrams() { return unitMg / 1000.0; }
    public IngredientCategory category() { return category; }

    public ResourceLocation rl() {
        return ResourceLocation.fromNamespaceAndPath("boulanger", id);
    }

    private static final Map<String, BakeryAdditiveType> BY_ID =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(BakeryAdditiveType::id, e -> e));

    public static Optional<BakeryAdditiveType> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public BakeryAdditiveComponent toComponent() {
        return new BakeryAdditiveComponent(id);
    }
}
