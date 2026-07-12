package net.boulangermod.boulanger.pneumatic.network;

import it.unimi.dsi.fastutil.longs.*;

import net.boulangermod.boulanger.block.pneumatic.DuctSide;
import net.boulangermod.boulanger.block.pneumatic.PneumaticDuctBlock;
import net.boulangermod.boulanger.block.pneumatic.ValveDuctBlock;
import net.boulangermod.boulanger.pneumatic.api.IAirEndpoint;
import net.boulangermod.boulanger.pneumatic.blockentity.AirCompressorBlockEntity;
import net.boulangermod.boulanger.pneumatic.blockentity.AirTankBlockEntity;
import net.boulangermod.boulanger.pneumatic.blockentity.PneumaticDuctBlockEntity;
import net.boulangermod.boulanger.pneumatic.debug.PneumaticDebug;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AirTopologyRebuilder {

    private final Long2ObjectOpenHashMap<AirNetwork> networks;
    private final Long2ObjectOpenHashMap<LongSet> netMembers;
    private final Long2ObjectOpenHashMap<List<EndpointLink>> netEndpoints;
    private final Long2BooleanOpenHashMap netVented;
    private final Long2DoubleOpenHashMap pendingSplitPressureKpa = new Long2DoubleOpenHashMap();

    private long nextId = 1;

    AirTopologyRebuilder(Long2ObjectOpenHashMap<AirNetwork> networks,
                         Long2ObjectOpenHashMap<LongSet> netMembers,
                         Long2ObjectOpenHashMap<List<EndpointLink>> netEndpoints,
                         Long2BooleanOpenHashMap netVented) {
        this.networks = networks;
        this.netMembers = netMembers;
        this.netEndpoints = netEndpoints;
        this.netVented = netVented;
    }

    /**
     * Rebuilds the connected component reachable from seed and creates/updates a network.
     *
     * @return member duct positions for rebuilt component, or null if seed isn't a duct or already covered this tick.
     */
    LongOpenHashSet rebuildFromSeed(Level level, BlockPos seed, LongOpenHashSet coveredDucts) {
        PneumaticDuctBlockEntity start = getDuct(level, seed);
        if (start == null) return null;

        long startLong = seed.asLong();
        if (coveredDucts.contains(startLong)) return null;

        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        queue.enqueue(startLong);
        visited.add(startLong);

        LongOpenHashSet memberDucts = new LongOpenHashSet();
        LongOpenHashSet oldIds = new LongOpenHashSet();

        boolean vented = false;
        double totalVolumeL = 0.0;

        // Any ducts/endpoints with prev network id == 0 are "new" to the simulation; treat them as containing
        // atmospheric air at join time (i.e., contribute ATM * V to PV) so connecting/disconnecting doesn't
        // spuriously drop below ATM then jump above it.
        double newAtmVolumeL = 0.0;

        ArrayList<EndpointHit> endpointHits = new ArrayList<>();
        LongOpenHashSet endpointSeen = new LongOpenHashSet();

        while (!queue.isEmpty()) {
            long curLong = queue.dequeueLong();
            BlockPos cur = BlockPos.of(curLong);

            PneumaticDuctBlockEntity duct = getDuct(level, cur);
            if (duct != null) {
                memberDucts.add(curLong);
                totalVolumeL += AirConstants.DUCT_VOLUME_L;

                long prevId = duct.getAirNetworkId();
                if (prevId != 0) {
                    oldIds.add(prevId);
                } else {
                    newAtmVolumeL += AirConstants.DUCT_VOLUME_L;
                }

                BlockState ductState = level.getBlockState(cur);
                if (!(ductState.getBlock() instanceof PneumaticDuctBlock)) {
                    continue;
                }

                // If THIS duct is a closed valve, it is a topology break.
                // Do not traverse out of it (neighbors also refuse to traverse *into* it).
                if (ductState.getBlock() instanceof ValveDuctBlock
                        && !ductState.getValue(ValveDuctBlock.OPEN)) {
                    continue;
                }

                for (Direction dir : Direction.values()) {
                    DuctSide side = ductState.getValue(PneumaticDuctBlock.propFor(dir));
                    if (side == DuctSide.CLOSED || side == DuctSide.FLANGED) continue;
                    if (duct.isSideBlocked(dir)) continue;

                    BlockPos nextPos = cur.relative(dir);
                    long nextLong = nextPos.asLong();

                    PneumaticDuctBlockEntity neighborDuct = getDuct(level, nextPos);
                    if (neighborDuct != null) {
                        BlockState nState = level.getBlockState(nextPos);
                        if (nState.getBlock() instanceof PneumaticDuctBlock) {
                            DuctSide nSide = nState.getValue(PneumaticDuctBlock.propFor(dir.getOpposite()));
                            if (nSide == DuctSide.CLOSED || nSide == DuctSide.FLANGED) continue;

                            // Closed valve blocks topology.
                            if (nState.getBlock() instanceof ValveDuctBlock) {
                                if (!nState.getValue(ValveDuctBlock.OPEN)) continue;
                            }

                            if (visited.add(nextLong)) queue.enqueue(nextLong);
                        }
                        continue;
                    }

                    BlockEntity be = level.getBlockEntity(nextPos);

                    // Tank as endpoint (also acts as bridge later)
                    if (be instanceof AirTankBlockEntity tank) {
                        Direction tankSide = dir.getOpposite();
                        if (endpointSeen.add(nextLong)) {
                            double v = tank.getAirVolumeLiters(tankSide);
                            if (v > 0.0) totalVolumeL += v;

                            // Newly placed tanks (id==0) start at atmosphere.
                            if (v > 0.0 && tank.getAirNetworkId() == 0) {
                                newAtmVolumeL += v;
                            }

                            endpointHits.add(new EndpointHit(tank, nextPos, tankSide));
                        }

                        if (visited.add(nextLong)) queue.enqueue(nextLong);
                        continue;
                    }

                    // Other endpoints
                    if (be instanceof IAirEndpoint endpoint) {
                        Direction endpointSide = dir.getOpposite();
                        if (endpointSeen.add(nextLong)) {
                            double v = endpoint.getAirVolumeLiters(endpointSide);
                            if (v > 0.0) totalVolumeL += v;
                            endpointHits.add(new EndpointHit(endpoint, nextPos, endpointSide));
                        }
                        continue;
                    }

                    // No endpoint: OPEN/CONNECTED face into AIR vents
                    if (side == DuctSide.OPEN || side == DuctSide.CONNECTED) {
                        BlockState ns = level.getBlockState(nextPos);
                        if (ns.isAir()) vented = true;
                    }
                }
                continue;
            }

            // Bridging endpoint: allow Air Tanks to connect endpoints to ducts.
            BlockEntity curBe = level.getBlockEntity(cur);
            if (curBe instanceof AirTankBlockEntity tank) {
                long prevId = tank.getAirNetworkId();
                if (prevId != 0) oldIds.add(prevId);

                for (Direction dir : Direction.values()) {
                    BlockPos nextPos = cur.relative(dir);

                    PneumaticDuctBlockEntity neighborDuct = getDuct(level, nextPos);
                    if (neighborDuct != null) {
                        BlockState nState = level.getBlockState(nextPos);
                        if (nState.getBlock() instanceof PneumaticDuctBlock) {
                            DuctSide nSide = nState.getValue(PneumaticDuctBlock.propFor(dir.getOpposite()));
                            if (nSide != DuctSide.CLOSED && nSide != DuctSide.FLANGED) {
                                if (nState.getBlock() instanceof ValveDuctBlock) {
                                    if (!nState.getValue(ValveDuctBlock.OPEN)) continue;
                                }
                                long nextLong = nextPos.asLong();
                                if (visited.add(nextLong)) queue.enqueue(nextLong);
                            }
                        }
                        continue;
                    }

                    BlockEntity be = level.getBlockEntity(nextPos);

                    // Allow tank-to-tank bridging too.
                    if (be instanceof AirTankBlockEntity otherTank) {
                        Direction otherSide = dir.getOpposite();
                        long otherLong = nextPos.asLong();

                        if (endpointSeen.add(otherLong)) {
                            double v = otherTank.getAirVolumeLiters(otherSide);
                            if (v > 0.0) totalVolumeL += v;

                            // Newly placed tanks (id==0) start at atmosphere.
                            if (v > 0.0 && otherTank.getAirNetworkId() == 0) {
                                newAtmVolumeL += v;
                            }

                            endpointHits.add(new EndpointHit(otherTank, nextPos, otherSide));
                        }

                        if (visited.add(otherLong)) queue.enqueue(otherLong);
                        continue;
                    }

                    // Other endpoints adjacent to tank
                    if (be instanceof IAirEndpoint endpoint) {
                        Direction endpointSide = dir.getOpposite();
                        long epLong = nextPos.asLong();

                        if (endpointSeen.add(epLong)) {
                            double v = endpoint.getAirVolumeLiters(endpointSide);
                            if (v > 0.0) totalVolumeL += v;
                            endpointHits.add(new EndpointHit(endpoint, nextPos, endpointSide));
                        }
                    }
                }
            }
        }

        if (memberDucts.isEmpty()) return null;

        // Choose id:
        long newId;
        boolean splitDetected = false;

        long singleOldId = 0;
        if (oldIds.size() == 1) {
            singleOldId = oldIds.iterator().nextLong();
            if (singleOldId == 0) {
                newId = nextId++;
            } else {
                // Split detection: if any prior member still exists outside this rebuilt set and still claims oldId
                LongSet priorMembers = netMembers.get(singleOldId);
                if (priorMembers != null) {
                    for (LongIterator pit = priorMembers.iterator(); pit.hasNext(); ) {
                        long pLong = pit.nextLong();
                        if (memberDucts.contains(pLong)) continue;

                        PneumaticDuctBlockEntity other = getDuct(level, BlockPos.of(pLong));
                        if (other != null && other.getAirNetworkId() == singleOldId) {
                            splitDetected = true;
                            break;
                        }
                    }
                }

                newId = splitDetected ? nextId++ : singleOldId;
            }
        } else if (oldIds.isEmpty()) {
            newId = nextId++;
        } else {
            // Merge: pick first as survivor, retire rest
            newId = oldIds.iterator().nextLong();
            if (newId == 0) newId = nextId++;
        }

        // Decide how much air (PV) this rebuilt component should start with.
        double desiredPv = AirConstants.ATM_KPA * totalVolumeL;

        // MERGE: conserve PV by summing PV from all merged networks, and assume any extra discovered volume
        // (e.g., newly placed ducts/tanks that had no prior reservoir) starts at atmosphere.
        if (oldIds.size() > 1) {
            desiredPv = 0.0;
            double summedOldVol = 0.0;
            for (LongIterator it = oldIds.iterator(); it.hasNext(); ) {
                long oid = it.nextLong();
                if (oid == 0) continue;
                AirNetwork on = networks.get(oid);
                if (on != null) {
                    desiredPv += on.pv();
                    summedOldVol += on.volumeLiters();
                }
            }

            double extraVol = totalVolumeL - summedOldVol;
            if (extraVol > 1.0e-6) {
                desiredPv += AirConstants.ATM_KPA * extraVol;
            }

            if (desiredPv <= 0.0) desiredPv = AirConstants.ATM_KPA * totalVolumeL;
        }

        // SINGLE OLD ID: reuse, split, or post-split remainder
        if (oldIds.size() == 1 && singleOldId != 0) {
            AirNetwork oldNet = networks.get(singleOldId);

            // Pre-split pressure (absolute), derived from PV / V
            double p0 = AirConstants.ATM_KPA;
            if (oldNet != null && oldNet.volumeLiters() > 1.0e-9) {
                p0 = oldNet.pv() / oldNet.volumeLiters();
            }

            if (splitDetected) {
                // This rebuilt component is one half of a split: keep same pressure as old network.
                pendingSplitPressureKpa.put(singleOldId, p0);
                desiredPv = p0 * totalVolumeL;
            } else if (newId == singleOldId && pendingSplitPressureKpa.containsKey(singleOldId)) {
                // This rebuilt component is the "other half" of the split (the one reusing oldId).
                double pSplit = pendingSplitPressureKpa.remove(singleOldId);
                desiredPv = pSplit * totalVolumeL;
            } else if (newId == singleOldId && oldNet != null && oldNet.volumeLiters() > 1.0e-9 && oldNet.pv() > 0.0) {
                // Normal rebuild reusing same id:
                // - If volume increased: treat the added volume as atmospheric air being connected.
                // - If volume decreased (or split detection missed due to rebuild ordering): assume removed
                //   volume took its air at the prior pressure; preserve pressure to avoid spikes.
                double oldV = oldNet.volumeLiters();
                double oldPv = oldNet.pv();
                double epsV = 1.0e-6;

                if (totalVolumeL > oldV + epsV) {
                    desiredPv = oldPv + (AirConstants.ATM_KPA * (totalVolumeL - oldV));
                } else {
                    desiredPv = p0 * totalVolumeL;
                }
            } else if (newId == singleOldId && oldNet != null && oldNet.volumeLiters() > 1.0e-9) {
                // Fallback: if PV is zero/uninitialized but we have a stable prior volume, preserve pressure.
                desiredPv = p0 * totalVolumeL;
            } else {
                // New id without split context, or no old net: atmosphere.
                desiredPv = AirConstants.ATM_KPA * totalVolumeL;
            }
        }

        // Enforce minimum absolute pressure at atmosphere (no vacuum simulation yet).
        // This also guards against transient under-ATM values from topology rebuild ordering.
        double minPv = AirConstants.ATM_KPA * totalVolumeL;
        if (desiredPv < minPv) desiredPv = minPv;

        // Create / reuse network object and set reservoir once.
        AirNetwork net = networks.get(newId);
        if (net == null) {
            net = new AirNetwork(newId);
            networks.put(newId, net);
        }
        net.setReservoir(totalVolumeL, desiredPv);

// Vented flag
        netVented.put(newId, vented);


        // Update membership sets
        networks.put(newId, net);
        netMembers.put(newId, memberDucts);

        // Store endpoints list (pos+side) for snapshot refresh & assignment
        List<EndpointLink> endpointLinks = new ArrayList<>(endpointHits.size());
        net.clearEndpoints();

        for (EndpointHit hit : endpointHits) {
            endpointLinks.add(new EndpointLink(hit.pos.asLong(), (byte) hit.side.ordinal()));
            net.addEndpoint(hit.endpoint, hit.pos, hit.side);

            BlockEntity be = level.getBlockEntity(hit.pos);
            if (be instanceof AirTankBlockEntity tank) {
                tank.setAirNetworkId(newId);
                if (PneumaticDebug.logAssignments()) {
                    PneumaticDebug.assign("net={} tank@{} side={} ep={}", newId, hit.pos, hit.side, hit.endpoint.debugName(hit.side));
                }
            } else if (be instanceof AirCompressorBlockEntity comp) {
                comp.setAirNetworkId(newId);
                if (PneumaticDebug.logAssignments()) {
                    PneumaticDebug.assign("net={} comp@{} side={} ep={}", newId, hit.pos, hit.side, hit.endpoint.debugName(hit.side));
                }
            } else if (be instanceof IAirEndpoint) {
                if (PneumaticDebug.logAssignments()) {
                    PneumaticDebug.assign("net={} ep@{} side={} ep={}", newId, hit.pos, hit.side, hit.endpoint.debugName(hit.side));
                }
            }
        }

        netEndpoints.put(newId, endpointLinks);

        // Assign duct membership + debug
        int ductCount = memberDucts.size();
        int endpointCount = endpointLinks.size();

        for (LongIterator it = memberDucts.iterator(); it.hasNext(); ) {
            long dLong = it.nextLong();
            BlockPos p = BlockPos.of(dLong);
            PneumaticDuctBlockEntity d = getDuct(level, p);
            if (d != null) {
                d.setAirNetworkId(newId);

                // At assignment time we usually don't have per-duct flow yet.
                // Items summary comes from the duct itself (safe on server).
                String items = d.getDebugDuctItemsSummary();
                d.setDebugSnapshot(net.pressureKpa(), net.volumeLiters(), ductCount, endpointCount, null, items);

                if (PneumaticDebug.logAssignments()) {
                    PneumaticDebug.assign("net={} duct@{}", newId, p);
                }
            }

        }

        // On merge, retire other old ids
        if (oldIds.size() > 1) {
            for (LongIterator it = oldIds.iterator(); it.hasNext(); ) {
                long oid = it.nextLong();
                if (oid == newId) continue;

                networks.remove(oid);
                netMembers.remove(oid);
                netEndpoints.remove(oid);
                netVented.remove(oid);

                if (PneumaticDebug.logTopology()) {
                    PneumaticDebug.topology("merged old net {} into {}", oid, newId);
                }
            }
        }

        if (PneumaticDebug.logTopology()) {
            PneumaticDebug.topology("rebuild seed={} -> newNet={} ducts={} endpoints={} V={}L P={}kPa vented={}",
                    seed, newId, ductCount, endpointCount,
                    String.format(Locale.ROOT, "%.2f", totalVolumeL),
                    String.format(Locale.ROOT, "%.2f", net.pressureKpa()),
                    vented
            );
        }

        return memberDucts;
    }

    void sweepStaleNetworks(Level level) {
        LongOpenHashSet toRemove = new LongOpenHashSet();

        for (LongIterator it = networks.keySet().iterator(); it.hasNext(); ) {
            long id = it.nextLong();

            LongSet members = netMembers.get(id);
            if (members == null || members.isEmpty()) {
                toRemove.add(id);
                continue;
            }

            boolean alive = false;
            for (LongIterator mit = members.iterator(); mit.hasNext(); ) {
                long pLong = mit.nextLong();
                PneumaticDuctBlockEntity d = getDuct(level, BlockPos.of(pLong));
                if (d != null && d.getAirNetworkId() == id) {
                    alive = true;
                    break;
                }
            }

            if (!alive) toRemove.add(id);
        }

        for (LongIterator rem = toRemove.iterator(); rem.hasNext(); ) {
            long id = rem.nextLong();
            networks.remove(id);
            netMembers.remove(id);
            netEndpoints.remove(id);
            netVented.remove(id);

            if (PneumaticDebug.logTopology()) {
                PneumaticDebug.topology("removed stale net {}", id);
            }
        }
    }

    private PneumaticDuctBlockEntity getDuct(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof PneumaticDuctBlockEntity duct) ? duct : null;
    }

    private record EndpointHit(IAirEndpoint endpoint, BlockPos pos, Direction side) {}
}
