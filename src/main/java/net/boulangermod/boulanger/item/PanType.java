package net.boulangermod.boulanger.item;

import net.minecraft.resources.ResourceLocation;

public enum PanType {
    LOAF("loaf", 1),
    BAGUETTE("baguette", 2);

    private final String id;
    private final int modelIndex;

    PanType(String id, int modelIndex) {
        this.id = id;
        this.modelIndex = modelIndex;
    }

    /** The simple name (matches the JSON/datagen `pan_type` value). */
    public String getId() {
        return id;
    }

    /** Used for model overrides in your item JSON. */
    public int getModelIndex() {
        return modelIndex;
    }

    /**
     * Lookup by the simple string ID.
     * @param id the path part of a ResourceLocation (e.g. "baguette" not "boulanger:baguette")
     */
    public static PanType fromId(String id) {
        for (PanType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return LOAF; // fallback default
    }

    /**
     * Lookup by a full ResourceLocation, matching on its path.
     * @param loc the namespaced ID (e.g. boulanger:baguette)
     */
    public static PanType byId(ResourceLocation loc) {
        if (loc == null) return LOAF;
        return fromId(loc.getPath());
    }
}
