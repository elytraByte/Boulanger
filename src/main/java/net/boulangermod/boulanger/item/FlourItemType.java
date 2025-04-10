package net.boulangermod.boulanger.item;

import net.boulangermod.boulanger.component.FlourType;

public enum FlourItemType {

    BREAK_FLOUR_1      ("first_break_flour",      11.0f, 0.5f, 0, 113f),
    BREAK_FLOUR_2      ("second_break_flour",     12.5f, 0.6f, 1, 113f),
    BREAK_FLOUR_3      ("third_break_flour",      13.5f, 0.7f, 2, 113f),
    BREAK_FLOUR_4      ("fourth_break_flour",     14.0f, 0.8f, 3, 113f),
    BREAK_FLOUR_5      ("fifth_break_flour",      16.0f, 1.0f, 4, 113f),
    MIDDLINGS_FLOUR_1  ("first_middlings_flour",  13.0f, 0.35f,5, 113f),
    MIDDLINGS_FLOUR_2  ("second_middlings_flour", 14.0f, 0.4f, 6, 113f),
    MIDDLINGS_FLOUR_3  ("third_middlings_flour",  15.0f, 0.45f,7, 113f),
    MIDDLINGS_FLOUR_4  ("fourth_middlings_flour", 15.5f, 0.5f, 8, 113f),
    MIDDLINGS_FLOUR_5  ("fifth_middlings_flour",  16.0f, 0.6f, 9, 113f),
    PATENT_FLOUR_1     ("first_patent_flour",     12.7f, 0.5f, 10,113f),
    PATENT_FLOUR_2     ("second_patent_flour",    11.0f, 0.7f, 11,113f),
    ALL_PURPOSE_FLOUR  ("all_purpose_flour",      11.7f, 0.52f,12,113f),
    HIGH_GLUTEN_FLOUR  ("high_gluten_flour",      14.2f, 0.5f, 13,113f),
    BREAD_FLOUR        ("bread_flour",            12.7f, 0.5f, 14,113f),
    SEMOLINA_FLOUR     ("semolina_flour",         13.0f, 0.9f, 15,113f),
    RYE_FLOUR          ("rye_flour",              7.0f,  1.6f, 16,113f),
    WHOLE_WHEAT_FLOUR  ("whole_wheat_flour",      11.7f, 1.2f, 17,113f),
    VITAL_WHEAT_GLUTEN ("vital_wheat_gluten",     90f,   0.7f, 18,113f),
    BRAN               ("wheat_bran",             0.0f,  0.7f, 19,113f);
    // GERM(…), etc.

    private final String id;
    private final float proteinContent;
    private final float ashContent;
    private final int   modelIndex;
    private final float weight;           // ← new field

    FlourItemType(String id, float proteinContent, float ashContent, int modelIndex, float weight) {
        this.id             = id;
        this.proteinContent = proteinContent;
        this.ashContent     = ashContent;
        this.modelIndex     = modelIndex;
        this.weight         = weight;
    }

    public String getId()               { return id; }
    public float  getProteinContent()   { return proteinContent; }
    public float  getAshContent()       { return ashContent; }
    public int    getModelIndex()       { return modelIndex; }
    public float  getWeight()           { return weight; }      // ← new getter

    public FlourType toFlourType() {
        // if FlourType has been updated to accept weight:
        return new FlourType(id, ashContent, proteinContent, modelIndex, weight);
        // otherwise you can pass weight separately when you register the component
    }

    public static FlourItemType fromId(String id) {
        for (FlourItemType type : values()) {
            if (type.getId().equals(id)) return type;
        }
        return BREAK_FLOUR_1;
    }
}
