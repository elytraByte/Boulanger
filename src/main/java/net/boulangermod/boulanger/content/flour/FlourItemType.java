package net.boulangermod.boulanger.content.flour;


public enum FlourItemType {

    BREAK_FLOUR_1      ("first_break_flour",      11.0f, 0.5f,  0, 113000, false),
    BREAK_FLOUR_2      ("second_break_flour",     12.5f, 0.6f,  1, 113000, false),
    BREAK_FLOUR_3      ("third_break_flour",      13.5f, 0.7f,  2, 113000, false),
    BREAK_FLOUR_4      ("fourth_break_flour",     14.0f, 0.8f,  3, 113000, false),
    BREAK_FLOUR_5      ("fifth_break_flour",      16.0f, 1.0f,  4, 113000, false),
    MIDDLINGS_FLOUR_1  ("first_middlings_flour",  13.0f, 0.35f, 5, 113000, false),
    MIDDLINGS_FLOUR_2  ("second_middlings_flour", 14.0f, 0.4f,  6, 113000, false),
    MIDDLINGS_FLOUR_3  ("third_middlings_flour",  15.0f, 0.45f, 7, 113000, false),
    MIDDLINGS_FLOUR_4  ("fourth_middlings_flour", 15.5f, 0.5f,  8, 113000, false),
    MIDDLINGS_FLOUR_5  ("fifth_middlings_flour",  16.0f, 0.6f,  9, 113000, false),
    PATENT_FLOUR_1     ("first_patent_flour",     12.7f, 0.5f, 10, 113000, true),
    PATENT_FLOUR_2     ("second_patent_flour",    11.0f, 0.7f, 11, 113000, true),
    ALL_PURPOSE_FLOUR  ("all_purpose_flour",      11.7f, 0.52f,12, 113000, true),
    HIGH_GLUTEN_FLOUR  ("high_gluten_flour",      14.2f, 0.5f, 13, 113000, true),
    BREAD_FLOUR        ("bread_flour",            12.7f, 0.5f, 14, 113000, true),
    SEMOLINA_FLOUR     ("semolina_flour",         13.0f, 0.9f, 15, 113000, true),
    RYE_FLOUR          ("rye_flour",              7.0f,  1.6f, 16, 113000, true),
    WHOLE_WHEAT_FLOUR  ("whole_wheat_flour",      11.7f, 1.2f, 17, 113000, true),
    VITAL_WHEAT_GLUTEN ("vital_wheat_gluten",     90f,   0.7f, 18, 113000, true),
    BRAN               ("wheat_bran",             0.0f,  0.7f, 19, 113000, true),
    CORN_MEAL          ("corn_meal",              0.0f,  0.0f, 20, 113000, true),
    CORN_STARCH        ("corn_starch",            0.0f,  0.0f, 21, 113000, true);

    private final String id;
    private final float proteinContent;
    private final float ashContent;
    private final int   modelIndex;
    private final int weightMg;
    private final boolean baggable;

    FlourItemType(String id, float proteinContent, float ashContent, int modelIndex, int weightMg, boolean baggable) {
        this.id = id;
        this.proteinContent = proteinContent;
        this.ashContent = ashContent;
        this.modelIndex = modelIndex;
        this.weightMg = weightMg;
        this.baggable = baggable;
    }

    public String getId()               { return id; }
    public float  getProteinContent()   { return proteinContent; }
    public float  getAshContent()       { return ashContent; }
    public int    getModelIndex()       { return modelIndex; }
    public int getWeightMg()           { return weightMg; }
    public boolean isBaggable()         { return baggable; }

    public FlourType toFlourType() {
        return new FlourType(id, ashContent, proteinContent, modelIndex, weightMg);
    }

    public static FlourItemType fromId(String id) {
        for (FlourItemType type : values()) {
            if (type.getId().equals(id)) return type;
        }
        return BREAK_FLOUR_1;
    }
}
