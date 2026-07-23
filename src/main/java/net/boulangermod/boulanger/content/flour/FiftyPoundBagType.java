package net.boulangermod.boulanger.content.flour;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.resources.ResourceLocation;

public enum FiftyPoundBagType {
    
    ALL_PURPOSE     (id("all_purpose_flour"), 101, 0.52f, 11.7f),
    BREAD           (id("bread_flour"), 102, 0.5f, 12.7f),
    HIGH_GLUTEN     (id("high_gluten_flour"), 103, 0.50f, 14.2f),
    RYE             (id("rye_flour"), 104, 1.6f, 7.0f),
    SEMOLINA        (id("semolina_flour"), 105, 0.90f, 13.0f),
    VWG             (id("vital_wheat_gluten"), 106, 0.7f, 90.0f),
    WHOLE_WHEAT     (id("whole_wheat_flour"), 107, 1.2f, 11.7f);

    public static final long MAX_MILLIGRAMS = 22680000L;

    private final ResourceLocation id;
    private final int modelIndex;
    private final float ash;
    private final float protein;

    FiftyPoundBagType(ResourceLocation id, int modelIndex, float ash, float protein) {
        this.id = id;
        this.modelIndex = modelIndex;
        this.ash = ash;
        this.protein = protein;
    }

    public ResourceLocation id() { return id; }
    public int modelIndex() { return modelIndex; }
    public float ash() { return ash; }
    public float protein() { return protein; }

    public FlourType toFlourType() {
        return new FlourType(id.toString(), ash, protein, modelIndex, MAX_MILLIGRAMS);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Boulanger.MOD_ID, path);
    }
}
