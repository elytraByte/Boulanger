package net.boulangermod.boulanger.pneumatic.network;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.boulangermod.boulanger.pneumatic.debug.PneumaticDebug;
import net.boulangermod.boulanger.pneumatic.item.PneumaticItemTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Orchestrator per Level:
 * - queues topology rebuild seeds
 * - ticks networks (leak/vent + endpoint tick)
 * - recomputes flow field when topology settles
 * - ticks item transport
 * - refreshes debug snapshots (rate-limited)
 */
public final class AirNetworkManager {

    private static final Map<Level, AirNetworkManager> INSTANCES = new WeakHashMap<>();

    public static AirNetworkManager get(Level level) {
        return INSTANCES.computeIfAbsent(level, AirNetworkManager::new);
    }

    // Networks by id
    final Long2ObjectOpenHashMap<AirNetwork> networks = new Long2ObjectOpenHashMap<>();

    // Last-known duct membership by network id (used for split detection + stale sweep)
    final Long2ObjectOpenHashMap<LongSet> netMembers = new Long2ObjectOpenHashMap<>();
    // Last-known endpoints per network (pos + side), used for snapshot refresh and flow sources
    final Long2ObjectOpenHashMap<List<EndpointLink>> netEndpoints = new Long2ObjectOpenHashMap<>();
    // Whether a network is vented to atmosphere
    final Long2BooleanOpenHashMap netVented = new Long2BooleanOpenHashMap();

    // Positions that need topology rebuild (seed + its 6 neighbors)
    private final LongSet rebuildQueue = new LongOpenHashSet();

    // Helpers (each owns one responsibility)
    private final AirTopologyRebuilder topology = new AirTopologyRebuilder(networks, netMembers, netEndpoints, netVented);
    private final AirFlowFieldSolver flowSolver = new AirFlowFieldSolver(netMembers, netEndpoints);

    private final PneumaticItemTransport itemTransport = new PneumaticItemTransport(flowSolver);

    // Whatever your project calls this class (you already referenced it in your file)
    private final AirDebugSnapshotUpdater debugUpdater =
            new AirDebugSnapshotUpdater(networks, netMembers, netEndpoints, flowSolver);

    private boolean flowDirty = true;

    private AirNetworkManager(Level level) {
        // nothing else required
    }

    public void enqueueTopologyChange(BlockPos pos) {
        flowDirty = true;

        // Only log the “seed” enqueue (not every neighbor) and only when newly added.
        boolean added = rebuildQueue.add(pos.asLong());
        for (Direction d : Direction.values()) {
            rebuildQueue.add(pos.relative(d).asLong());
        }

        if (added && PneumaticDebug.logTopology()) {
            PneumaticDebug.topology("enqueue topology @ {}", pos);
        }
    }

    public Direction getItemFlowDirection(BlockPos ductPos) {
        return flowSolver.getItemFlowDirection(ductPos);
    }

    /** Optional convenience wrapper if you want this elsewhere. */
    public boolean canAirPass(Level level, BlockPos from, Direction dir) {
        return flowSolver.canAirPass(level, from, dir);
    }

    public PneumaticItemTransport itemTransport() {
        return itemTransport;
    }
    public void serverTick(Level level) {
        if (level.isClientSide) return;

        final long gameTime = level.getGameTime();

        // 1) Process rebuild queue with per-tick component dedupe
        int processedSeeds = 0;
        int rebuiltComponents = 0;

        LongOpenHashSet coveredDucts = new LongOpenHashSet();
        int seedBudget = 256;

        LongIterator it = rebuildQueue.iterator();
        while (it.hasNext() && seedBudget-- > 0) {
            long seedLong = it.nextLong();
            it.remove();
            processedSeeds++;

            LongOpenHashSet memberDucts = topology.rebuildFromSeed(level, BlockPos.of(seedLong), coveredDucts);
            if (memberDucts != null) {
                rebuiltComponents++;
                coveredDucts.addAll(memberDucts);
            }
        }

        // If the queue has settled, we can safely sweep stale nets.
        if (processedSeeds > 0 && rebuildQueue.isEmpty()) {
            topology.sweepStaleNetworks(level);
        }

        // When topology settles, recompute directed flow for item transport.
        if (flowDirty && rebuildQueue.isEmpty()) {
            flowSolver.recomputeFlowField(level);
            flowDirty = false;
        }

        if (processedSeeds > 0 && PneumaticDebug.logTopology()) {
            PneumaticDebug.topology(
                    "tick={} processedSeeds={} rebuiltComponents={} remainingQueue={} networks={}",
                    gameTime, processedSeeds, rebuiltComponents, rebuildQueue.size(), networks.size()
            );
        }

        // 2) Tick networks (THIS is the missing piece: endpoints add/remove PV here)
        for (AirNetwork net : networks.values()) {
            net.tick(gameTime);

            // Vent/leak toward atmosphere if flagged vented
            if (netVented.get(net.id())) {
                double vol = net.volumeLiters();
                double pv = net.pv();
                double targetPv = AirConstants.ATM_KPA * vol;

                double r = AirConstants.VENT_RATE;
                pv += (targetPv - pv) * r;

                net.setReservoir(vol, pv);
            }
        }

        // 3) Tick item transport (moves PneumaticTravelingItem stacks through ducts)
        itemTransport.serverTick(level, gameTime, networks::get);

        // 4) Update overlay debug snapshots at a rate limit
        if (PneumaticDebug.overlayEnabled() && PneumaticDebug.rateLimit(gameTime)) {
            debugUpdater.update(level);
        }
    }
}
