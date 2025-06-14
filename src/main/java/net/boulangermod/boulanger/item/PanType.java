package net.boulangermod.boulanger.item;

public enum PanType {
    LOAF("loaf", 1),
    BAGUETTE("baguette", 2);

    private final String id;
    private final int modelIndex;

    PanType(String id, int modelIndex) {
        this.id = id;
        this.modelIndex = modelIndex;
    }

    public String getId() {
        return id;
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public static PanType fromId(String id) {
        for (PanType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return LOAF; // fallback
    }
}

