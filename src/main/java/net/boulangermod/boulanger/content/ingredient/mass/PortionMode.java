package net.boulangermod.boulanger.content.ingredient.mass;

/**
 * Describes how an ingredient source may be divided by the portioning system.
 */
public enum PortionMode {
    /**
     * The source consists of fixed-weight units. Whole units may be separated,
     * and a later portioning step may represent a partial final unit explicitly.
     */
    DISCRETE_UNITS,

    /**
     * The source stores its total mass directly and may be reduced by any whole
     * milligram amount.
     */
    VARIABLE_WEIGHT,

    /**
     * A unit must be consumed at its complete registered mass because the
     * remainder cannot currently be represented.
     */
    ALL_OR_NOTHING
}
