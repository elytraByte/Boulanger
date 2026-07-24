package net.boulangermod.boulanger.content.additive;

import net.boulangermod.boulanger.component.value.BakeryAdditiveComponent;
import net.boulangermod.boulanger.content.ingredient.IngredientCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum BakeryAdditiveType {
    ASCORBIC_ACID            ("ascorbic_acid",             454000L, IngredientCategory.ADDITIVE),
    CALCIUM_PROPIONATE       ("calcium_propionate",        454000L, IngredientCategory.ADDITIVE),
    DIASTATIC_MALT_POWDER    ("diastatic_malt_powder",     454000L, IngredientCategory.ADDITIVE),
    NONDIASTATIC_MALT_POWDER ("nondiastatic_malt_powder",  454000L, IngredientCategory.ADDITIVE),
    L_CYSTEINE               ("l_cysteine",                113000L, IngredientCategory.ADDITIVE),
    S_500_RED                ("s_500_red",                 22680000L, IngredientCategory.ADDITIVE),
    IM_PROVE_200             ("im_prove_200",              20412000L, IngredientCategory.ADDITIVE),
    ADVANTAGE_500_CL         ("advantage_500_cl",          10000000L, IngredientCategory.ADDITIVE);

    private final String id;
    private final long unitMg;
    private final IngredientCategory category;

    BakeryAdditiveType(String id, long unitMg, IngredientCategory category) {
        this.id = id;
        this.unitMg = unitMg;
        this.category = category;
    }

    public String id() { return id; }
    public long unitMg() { return unitMg; }
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
