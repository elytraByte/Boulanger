package net.boulangermod.boulanger.content.ingredient.mass;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry for fixed-mass ingredients that do not carry Boulanger identity
 * components. Mod integrations may register additional items during setup.
 */
public final class IngredientMassRegistry {
    private static final Map<Item, Entry> ENTRIES =
            Collections.synchronizedMap(new IdentityHashMap<>());

    static {
        registerUnit(Items.SUGAR, 113_000L);
        registerUnit(Items.HONEY_BOTTLE, 454_000L);

        // Preserve the existing scale's four-kilogram bucket convention.
        registerAllOrNothing(Items.WATER_BUCKET, 4_000_000L);
    }

    private IngredientMassRegistry() {}

    public static void registerUnit(ItemLike item, long unitMilligrams) {
        register(item, unitMilligrams, PortionMode.DISCRETE_UNITS);
    }

    public static void registerAllOrNothing(ItemLike item, long unitMilligrams) {
        register(item, unitMilligrams, PortionMode.ALL_OR_NOTHING);
    }

    static Optional<Entry> find(Item item) {
        return Optional.ofNullable(ENTRIES.get(item));
    }

    private static void register(ItemLike itemLike, long unitMilligrams, PortionMode portionMode) {
        Objects.requireNonNull(itemLike, "item");
        if (unitMilligrams <= 0) {
            throw new IllegalArgumentException("unitMilligrams must be positive");
        }
        if (portionMode == PortionMode.VARIABLE_WEIGHT) {
            throw new IllegalArgumentException(
                    "Variable-weight sources must use INGREDIENT_MILLIGRAMS"
            );
        }

        ENTRIES.put(itemLike.asItem(), new Entry(unitMilligrams, portionMode));
    }

    record Entry(long unitMilligrams, PortionMode portionMode) {}
}
