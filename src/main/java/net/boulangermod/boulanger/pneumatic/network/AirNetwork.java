package net.boulangermod.boulanger.pneumatic.network;

import net.boulangermod.boulanger.pneumatic.api.AirNetworkView;
import net.boulangermod.boulanger.pneumatic.api.IAirEndpoint;
import net.boulangermod.boulanger.pneumatic.debug.PneumaticDebug;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class AirNetwork {
    private final long id;

    private double totalVolumeLiters;
    private double pvKpaLiters;

    private final List<EndpointRef> endpoints = new ArrayList<>();

    public AirNetwork(long id) {
        this.id = id;
    }

    public long id() { return id; }

    public double pressureKpa() {
        if (totalVolumeLiters <= 0) return AirConstants.ATM_KPA;
        double p = pvKpaLiters / totalVolumeLiters;
        if (p < AirConstants.MIN_PRESSURE_KPA) p = AirConstants.MIN_PRESSURE_KPA;
        if (p > AirConstants.MAX_PRESSURE_KPA) p = AirConstants.MAX_PRESSURE_KPA;
        return p;
    }

    public double totalVolumeLiters() { return totalVolumeLiters; }
    public double pvKpaLiters() { return pvKpaLiters; }

    public int endpointCount() { return endpoints.size(); }

    public AirNetworkView view() {
        return new AirNetworkView(id, pressureKpa(), totalVolumeLiters, pvKpaLiters);
    }

    public double pv() {
        return this.pvKpaLiters;
    }

    public double volumeLiters() {
        return this.totalVolumeLiters;
    }

    public void setReservoir(double totalVolumeLiters, double pvKpaLiters) {
        this.totalVolumeLiters = Math.max(0.0, totalVolumeLiters);
        this.pvKpaLiters = Math.max(0.0, pvKpaLiters);
        clampPvToLimits();
    }

    private void clampPvToLimits() {
        if (totalVolumeLiters <= 1e-9) return;
        double maxPv = AirConstants.MAX_PRESSURE_KPA * totalVolumeLiters;
        if (pvKpaLiters > maxPv) pvKpaLiters = maxPv;
        if (pvKpaLiters < 0.0) pvKpaLiters = 0.0;
    }

    public void clearEndpoints() { endpoints.clear(); }

    public void addEndpoint(IAirEndpoint endpoint, BlockPos pos, Direction side) {
        endpoints.add(new EndpointRef(endpoint, pos, side));
    }

    /** Called each server tick by the manager if the network is active. */
    public void tick(long gameTime) {
        if (endpoints.isEmpty()) return;

        boolean doLog = PneumaticDebug.logNetworkTick() && PneumaticDebug.rateLimit(gameTime);
        boolean doEndpointLog = PneumaticDebug.logEndpointPv() && PneumaticDebug.rateLimit(gameTime);

        double beforeP = pressureKpa();
        double beforePV = pvKpaLiters;

        // Phase 1: sinks remove PV first
        AirNetworkView viewBefore = view();
        double pvRemove = 0.0;

        for (EndpointRef ref : endpoints) {
            double rem = Math.max(0.0, ref.endpoint.getPvRemovedThisTick(viewBefore, ref.side));
            pvRemove += rem;

            if (doEndpointLog && rem > 0.0) {
                PneumaticDebug.endpoint(
                        "net={} endpoint={} pos={} addPV=0.0 remPV={}",
                        id,
                        ref.endpoint.debugName(ref.side),
                        ref.pos,
                        rem
                );
            }
        }

        pvKpaLiters -= pvRemove;
        if (pvKpaLiters < 0.0) pvKpaLiters = 0.0;
        clampPvToLimits();

        // Phase 2: sources add PV after losses have been applied
        AirNetworkView viewAfterRemove = view();
        double pvAdd = 0.0;

        for (EndpointRef ref : endpoints) {
            double add = Math.max(0.0, ref.endpoint.getPvAddedThisTick(viewAfterRemove, ref.side));
            pvAdd += add;

            if (doEndpointLog && add > 0.0) {
                PneumaticDebug.endpoint(
                        "net={} endpoint={} pos={} addPV={} remPV=0.0",
                        id,
                        ref.endpoint.debugName(ref.side),
                        ref.pos,
                        add
                );
            }
        }

        pvKpaLiters += pvAdd;
        clampPvToLimits();

        if (doLog) {
            double afterP = pressureKpa();
            PneumaticDebug.tick(
                    "net={} P {:.2f}->{:.2f} kPa | V={:.2f} L | PV {:.2f} -> {:.2f} (Δ +{:.2f} -{:.2f}) | endpoints={}",
                    id, beforeP, afterP, totalVolumeLiters, beforePV, pvKpaLiters, pvAdd, pvRemove, endpoints.size()
            );
        }
    }


    private record EndpointRef(IAirEndpoint endpoint, BlockPos pos, Direction side) {}
}
