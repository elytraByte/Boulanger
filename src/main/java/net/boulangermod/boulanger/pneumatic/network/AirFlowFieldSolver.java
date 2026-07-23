//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.network;
//
//import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
//import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
//import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
//import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
//import it.unimi.dsi.fastutil.longs.LongIterator;
//import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
//import it.unimi.dsi.fastutil.longs.LongSet;
//
//import net.boulangermod.boulanger.block.pneumatic.DuctSide;
//import net.boulangermod.boulanger.block.pneumatic.OneWayValveDuctBlock;
//import net.boulangermod.boulanger.block.pneumatic.PneumaticDuctBlock;
//import net.boulangermod.boulanger.block.pneumatic.ValveDuctBlock;
//import net.boulangermod.boulanger.pneumatic.blockentity.AirCompressorBlockEntity;
//import net.boulangermod.boulanger.pneumatic.blockentity.PneumaticDuctBlockEntity;
//import net.boulangermod.boulanger.pneumatic.debug.PneumaticDebug;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//
//import java.util.List;
//
//public final class AirFlowFieldSolver {
//
//    private static final byte FLOW_NONE = (byte) 127;
//
//    private final Long2ByteOpenHashMap ductFlowDirOrd = new Long2ByteOpenHashMap();
//
//    private final Long2ObjectOpenHashMap<LongSet> netMembers;
//    private final Long2ObjectOpenHashMap<List<EndpointLink>> netEndpoints;
//
//    AirFlowFieldSolver(Long2ObjectOpenHashMap<LongSet> netMembers,
//                       Long2ObjectOpenHashMap<List<EndpointLink>> netEndpoints) {
//        this.netMembers = netMembers;
//        this.netEndpoints = netEndpoints;
//        ductFlowDirOrd.defaultReturnValue(FLOW_NONE);
//    }
//
//    public Direction getItemFlowDirection(BlockPos ductPos) {
//        byte ord = ductFlowDirOrd.get(ductPos.asLong());
//        if (ord == FLOW_NONE) return null;
//        return Direction.values()[ord];
//    }
//
//    public boolean canAirPass(Level level, BlockPos from, Direction dir) {
//        BlockPos to = from.relative(dir);
//
//        PneumaticDuctBlockEntity fromDuct = getDuct(level, from);
//        PneumaticDuctBlockEntity toDuct = getDuct(level, to);
//        if (fromDuct == null || toDuct == null) return false;
//
//        BlockState fromState = level.getBlockState(from);
//        BlockState toState = level.getBlockState(to);
//
//        return canTraverseFlowEdge(level, from, fromDuct, fromState, dir, to, toDuct, toState);
//    }
//
//    void recomputeFlowField(Level level) {
//        ductFlowDirOrd.clear();
//
//        // Nodes are all ducts in any network (disjoint union is fine)
//        LongOpenHashSet nodes = new LongOpenHashSet();
//        for (LongSet s : netMembers.values()) {
//            if (s != null) nodes.addAll(s);
//        }
//        if (nodes.isEmpty()) return;
//
//        // Sinks: ducts that are vented to atmosphere (any OPEN port into air)
//        LongOpenHashSet sinks = new LongOpenHashSet();
//        for (LongIterator it = nodes.iterator(); it.hasNext(); ) {
//            long dLong = it.nextLong();
//            BlockPos p = BlockPos.of(dLong);
//
//            PneumaticDuctBlockEntity duct = getDuct(level, p);
//            if (duct == null) continue;
//
//            BlockState state = level.getBlockState(p);
//            if (!isDuctBlock(state)) continue;
//            if (isValveClosed(state)) continue;
//
//            if (isVentedToAir(level, p, duct, state)) sinks.add(dLong);
//        }
//        if (sinks.isEmpty()) return;
//
//        // Sources: ducts adjacent to compressor endpoints.
//        LongOpenHashSet sources = new LongOpenHashSet();
//        for (List<EndpointLink> eps : netEndpoints.values()) {
//            if (eps == null) continue;
//            for (EndpointLink link : eps) {
//                BlockPos epPos = BlockPos.of(link.posLong());
//                BlockEntity be = level.getBlockEntity(epPos);
//                if (!(be instanceof AirCompressorBlockEntity)) continue;
//
//                Direction side = Direction.values()[link.sideOrd()];
//                BlockPos ductPos = epPos.relative(side);
//                long dLong = ductPos.asLong();
//                if (nodes.contains(dLong)) sources.add(dLong);
//            }
//        }
//        if (sources.isEmpty()) return;
//
//        // Forward reachability from sources over directed graph.
//        LongOpenHashSet reachableFromSource = new LongOpenHashSet();
//        LongArrayFIFOQueue fq = new LongArrayFIFOQueue();
//        for (LongIterator it = sources.iterator(); it.hasNext(); ) {
//            long sLong = it.nextLong();
//            if (reachableFromSource.add(sLong)) fq.enqueue(sLong);
//        }
//
//        while (!fq.isEmpty()) {
//            long curLong = fq.dequeueLong();
//            BlockPos curPos = BlockPos.of(curLong);
//
//            PneumaticDuctBlockEntity curDuct = getDuct(level, curPos);
//            if (curDuct == null) continue;
//
//            BlockState curState = level.getBlockState(curPos);
//            if (!isDuctBlock(curState) || isValveClosed(curState)) continue;
//
//            for (Direction dir : Direction.values()) {
//                BlockPos nextPos = curPos.relative(dir);
//                long nextLong = nextPos.asLong();
//                if (!nodes.contains(nextLong)) continue;
//
//                PneumaticDuctBlockEntity nextDuct = getDuct(level, nextPos);
//                if (nextDuct == null) continue;
//
//                BlockState nextState = level.getBlockState(nextPos);
//                if (!isDuctBlock(nextState) || isValveClosed(nextState)) continue;
//
//                if (canTraverseFlowEdge(level, curPos, curDuct, curState, dir, nextPos, nextDuct, nextState)) {
//                    if (reachableFromSource.add(nextLong)) fq.enqueue(nextLong);
//                }
//            }
//        }
//
//        // Reverse BFS: distance-to-sink over incoming directed edges.
//        Long2IntOpenHashMap dist = new Long2IntOpenHashMap();
//        dist.defaultReturnValue(-1);
//
//        LongArrayFIFOQueue rq = new LongArrayFIFOQueue();
//        for (LongIterator it = sinks.iterator(); it.hasNext(); ) {
//            long sLong = it.nextLong();
//            dist.put(sLong, 0);
//            rq.enqueue(sLong);
//        }
//
//        while (!rq.isEmpty()) {
//            long curLong = rq.dequeueLong();
//            int curDist = dist.get(curLong);
//
//            BlockPos curPos = BlockPos.of(curLong);
//            PneumaticDuctBlockEntity curDuct = getDuct(level, curPos);
//            if (curDuct == null) continue;
//
//            BlockState curState = level.getBlockState(curPos);
//            if (!isDuctBlock(curState) || isValveClosed(curState)) continue;
//
//            for (Direction dir : Direction.values()) {
//                BlockPos nbPos = curPos.relative(dir);
//                long nbLong = nbPos.asLong();
//                if (!nodes.contains(nbLong)) continue;
//                if (dist.get(nbLong) != -1) continue;
//
//                PneumaticDuctBlockEntity nbDuct = getDuct(level, nbPos);
//                if (nbDuct == null) continue;
//
//                BlockState nbState = level.getBlockState(nbPos);
//                if (!isDuctBlock(nbState) || isValveClosed(nbState)) continue;
//
//                Direction nbToCur = dir.getOpposite();
//                if (canTraverseFlowEdge(level, nbPos, nbDuct, nbState, nbToCur, curPos, curDuct, curState)) {
//                    dist.put(nbLong, curDist + 1);
//                    rq.enqueue(nbLong);
//                }
//            }
//        }
//
//        // Choose preferred outgoing direction per node (toward decreasing dist).
//        for (LongIterator it = nodes.iterator(); it.hasNext(); ) {
//            long curLong = it.nextLong();
//
//            if (!reachableFromSource.contains(curLong)) {
//                ductFlowDirOrd.put(curLong, FLOW_NONE);
//                continue;
//            }
//
//            int d = dist.get(curLong);
//            if (d <= 0) {
//                ductFlowDirOrd.put(curLong, FLOW_NONE);
//                continue;
//            }
//
//            BlockPos curPos = BlockPos.of(curLong);
//            PneumaticDuctBlockEntity curDuct = getDuct(level, curPos);
//            if (curDuct == null) {
//                ductFlowDirOrd.put(curLong, FLOW_NONE);
//                continue;
//            }
//
//            BlockState curState = level.getBlockState(curPos);
//            if (!isDuctBlock(curState) || isValveClosed(curState)) {
//                ductFlowDirOrd.put(curLong, FLOW_NONE);
//                continue;
//            }
//
//            byte best = FLOW_NONE;
//
//            for (Direction dir : Direction.values()) {
//                BlockPos nextPos = curPos.relative(dir);
//                long nextLong = nextPos.asLong();
//                if (!nodes.contains(nextLong)) continue;
//                if (!reachableFromSource.contains(nextLong)) continue;
//                if (dist.get(nextLong) != d - 1) continue;
//
//                PneumaticDuctBlockEntity nextDuct = getDuct(level, nextPos);
//                if (nextDuct == null) continue;
//
//                BlockState nextState = level.getBlockState(nextPos);
//                if (!isDuctBlock(nextState) || isValveClosed(nextState)) continue;
//
//                if (canTraverseFlowEdge(level, curPos, curDuct, curState, dir, nextPos, nextDuct, nextState)) {
//                    best = (byte) dir.get3DDataValue();
//                    break;
//                }
//            }
//
//            ductFlowDirOrd.put(curLong, best);
//        }
//
//        if (PneumaticDebug.logTopology()) {
//            PneumaticDebug.topology("recomputed flow field: nodes={} sources={} sinks={}", nodes.size(), sources.size(), sinks.size());
//        }
//    }
//
//    private static boolean isDuctBlock(BlockState state) {
//        return state.getBlock() instanceof PneumaticDuctBlock;
//    }
//
//    private static boolean isOneWayValve(BlockState state) {
//        return state.getBlock() instanceof OneWayValveDuctBlock;
//    }
//
//    private static boolean isValveClosed(BlockState state) {
//        return (state.getBlock() instanceof ValveDuctBlock) && !state.getValue(ValveDuctBlock.OPEN);
//    }
//
//    private static boolean isPortOpen(BlockState state, Direction dir) {
//        DuctSide s = state.getValue(PneumaticDuctBlock.propFor(dir));
//        return s != DuctSide.CLOSED && s != DuctSide.FLANGED;
//    }
//
//    private boolean isVentedToAir(Level level, BlockPos ductPos, PneumaticDuctBlockEntity duct, BlockState state) {
//        for (Direction dir : Direction.values()) {
//            if (!isPortOpen(state, dir)) continue;
//            if (duct.isSideBlocked(dir)) continue;
//
//            BlockState ns = level.getBlockState(ductPos.relative(dir));
//            if (ns.isAir()) return true;
//        }
//        return false;
//    }
//
//    /**
//     * Directed edge traversal rule-set for item transport.
//     */
//    private boolean canTraverseFlowEdge(Level level,
//                                        BlockPos fromPos, PneumaticDuctBlockEntity fromDuct, BlockState fromState,
//                                        Direction dir,
//                                        BlockPos toPos, PneumaticDuctBlockEntity toDuct, BlockState toState) {
//
//        if (!isDuctBlock(fromState) || !isDuctBlock(toState)) return false;
//
//        if (!isPortOpen(fromState, dir)) return false;
//        if (!isPortOpen(toState, dir.getOpposite())) return false;
//
//        if (fromDuct.isSideBlocked(dir)) return false;
//        if (toDuct.isSideBlocked(dir.getOpposite())) return false;
//
//        if (isValveClosed(fromState) || isValveClosed(toState)) return false;
//
//        // Exiting a one-way valve: only allow FLOW direction
//        if (isOneWayValve(fromState)) {
//            if (!fromState.getValue(ValveDuctBlock.OPEN)) return false;
//            Direction flow = fromState.getValue(OneWayValveDuctBlock.FLOW);
//            if (dir != flow) return false;
//        }
//
//        // Entering a one-way valve: must move INTO it along its FLOW direction
//        if (isOneWayValve(toState)) {
//            if (!toState.getValue(ValveDuctBlock.OPEN)) return false;
//            Direction flow = toState.getValue(OneWayValveDuctBlock.FLOW);
//            if (dir != flow) return false;
//        }
//
//        return true;
//    }
//
//    private PneumaticDuctBlockEntity getDuct(Level level, BlockPos pos) {
//        BlockEntity be = level.getBlockEntity(pos);
//        return (be instanceof PneumaticDuctBlockEntity duct) ? duct : null;
//    }
//}
