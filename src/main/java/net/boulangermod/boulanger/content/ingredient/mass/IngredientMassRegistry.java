package net.boulangermod.boulanger.content.ingredient.mass;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
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

        registerAllOrNothing(
                Items.WATER_BUCKET,
                4_000_000L,
                Items.BUCKET
        );
    }

    private IngredientMassRegistry() {}

    public static void registerUnit(ItemLike item, long unitMilligrams) {
        register(
                item,
                unitMilligrams,
                PortionMode.DISCRETE_UNITS,
                null
        );
    }

    public static void registerAllOrNothing(
            ItemLike item,
            long unitMilligrams
    ) {
        register(
                item,
                unitMilligrams,
                PortionMode.ALL_OR_NOTHING,
                null
        );
    }

    public static void registerAllOrNothing(
            ItemLike item,
            long unitMilligrams,
            ItemLike emptyContainer
    ) {
        register(
                item,
                unitMilligrams,
                PortionMode.ALL_OR_NOTHING,
                emptyContainer
        );
    }

    static Optional<Entry> find(Item item) {
        return Optional.ofNullable(ENTRIES.get(item));
    }

    private static void register(
            ItemLike itemLike,
            long unitMilligrams,
            PortionMode portionMode,
            @Nullable ItemLike emptyContainer
    ) {
        Objects.requireNonNull(itemLike, "itemLike");
        Objects.requireNonNull(portionMode, "portionMode");

        if (unitMilligrams <= 0) {
            throw new IllegalArgumentException(
                    "unit milligrams must be positive"
            );
        }

        if (portionMode == PortionMode.VARIABLE_WEIGHT) {
            throw new IllegalArgumentException(
                    "Variable-weight sources must use INGREDIENT_MILLIGRAMS"
            );
        }

        ENTRIES.put(
                itemLike.asItem(),
                new Entry(
                        unitMilligrams,
                        portionMode,
                        emptyContainer != null
                                ? emptyContainer.asItem()
                                : null
                )
        );
    }

    record Entry(
            long unitMilligrams,
            PortionMode portionMode,
            @Nullable Item emptyContainer
    ) {}
}
