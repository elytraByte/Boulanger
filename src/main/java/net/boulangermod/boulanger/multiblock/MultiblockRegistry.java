package net.boulangermod.boulanger.multiblock;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple registry to keep track of available multiblock definitions.
 */
public final class MultiblockRegistry {
    private static final Map<String, IMultiblock> REGISTRY = new LinkedHashMap<>();

    private MultiblockRegistry() {}

    public static void register(IMultiblock multiblock) {
        REGISTRY.put(multiblock.getUniqueName(), multiblock);
    }

    public static IMultiblock get(String name) {
        return REGISTRY.get(name);
    }

    public static Collection<IMultiblock> all() {
        return REGISTRY.values();
    }
}
