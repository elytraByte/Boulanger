package net.boulangermod.boulanger.pneumatic.api;

import net.minecraft.core.Direction;

/**
 * Implement this on any BlockEntity that should participate in the air network.
 *
 * v1 semantics:
 * - Endpoint contributes volume (buffers pressure).
 * - Endpoint may add/remove PV each tick (compressor/consumer later).
 */
public interface IAirEndpoint {

    /**
     * How much internal volume (liters) this endpoint contributes to the shared network reservoir.
     * Example: small port = 0.1L, tank = 50L, etc.
     */
    double getAirVolumeLiters(Direction side);

    /**
     * Optional: add air to the network this tick in PV units (kPa·L).
     * Compressor later returns positive values.
     */
    default double getPvAddedThisTick(AirNetworkView view, Direction side) {
        return 0.0;
    }

    /**
     * Optional: remove air from the network this tick in PV units (kPa·L).
     * Consumers / leaks later return positive values.
     */
    default double getPvRemovedThisTick(AirNetworkView view, Direction side) {
        return 0.0;
    }

    default String debugName(Direction side) {
        return getClass().getSimpleName() + "[" + side.getName() + "]";
    }
}