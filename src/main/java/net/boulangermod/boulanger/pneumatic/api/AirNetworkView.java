package net.boulangermod.boulanger.pneumatic.api;

public record AirNetworkView(
        long networkId,
        double pressureKpa,
        double totalVolumeLiters,
        double pvKpaLiters
) {}