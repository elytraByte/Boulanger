package net.boulangermod.boulanger.content.ingredient.mass;

import java.util.Objects;

/**
 * Resolved mass information for an ingredient stack.
 *
 * @param totalMilligrams total mass represented by the complete stack
 * @param unitMilligrams mass of one discrete unit, or one milligram for a
 *                        variable-weight source
 * @param portionMode how the source may be divided
 */
public record IngredientMass(
        long totalMilligrams,
        long unitMilligrams,
        PortionMode portionMode
) {
    public IngredientMass {
        if (totalMilligrams < 0) {
            throw new IllegalArgumentException("totalMilligrams must not be negative");
        }
        if (unitMilligrams <= 0) {
            throw new IllegalArgumentException("unitMilligrams must be positive");
        }
        Objects.requireNonNull(portionMode, "portionMode");
    }

    public boolean isEmpty() {
        return totalMilligrams == 0;
    }
}
