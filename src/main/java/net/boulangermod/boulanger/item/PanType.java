package net.boulangermod.boulanger.item;

import net.minecraft.resources.ResourceLocation;

public enum PanType {
    // Use multiples of 4 so every type gets a clean block of indices.
    LOAF("loaf", 0),
    BAGUETTE("baguette", 4);

    private final String id;
    /** Base (even) model index. Derived states are base+1 (full) and base+3 (proofed). */
    private final int baseModelIndex;

    PanType(String id, int baseModelIndex) {
        this.id = id;
        this.baseModelIndex = baseModelIndex;
    }

    /** The simple name (matches the JSON/datagen `pan_type` value). */
    public String getId() {
        return id;
    }

    /** Even: empty state (render empty pan). */
    public int getEmptyModelIndex() {
        return baseModelIndex;
    }

    /** Odd: full state (render pan with dough). */
    public int getFullModelIndex() {
        return baseModelIndex + 1;
    }

    /** Odd: proofed state (render pan with proofed dough). */
    public int getProofedModelIndex() {
        return baseModelIndex + 3;
    }

    /** If you still need the legacy single index, treat it as 'empty'. */
    public int getModelIndex() {
        return getEmptyModelIndex();
    }

    /** Expose the base in case you want to compute other variants later. */
    public int getBaseModelIndex() {
        return baseModelIndex;
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