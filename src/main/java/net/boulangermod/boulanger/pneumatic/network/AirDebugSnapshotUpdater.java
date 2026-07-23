//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.network;
//
//import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
//import it.unimi.dsi.fastutil.longs.LongIterator;
//import it.unimi.dsi.fastutil.longs.LongSet;
//
//import net.boulangermod.boulanger.pneumatic.blockentity.AirCompressorBlockEntity;
//import net.boulangermod.boulanger.pneumatic.blockentity.AirTankBlockEntity;
//import net.boulangermod.boulanger.pneumatic.blockentity.PneumaticDuctBlockEntity;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.entity.BlockEntity;
//
//import java.util.List;
//
//final class AirDebugSnapshotUpdater {
//
//    private final Long2ObjectOpenHashMap<AirNetwork> networks;
//    private final Long2ObjectOpenHashMap<LongSet> netMembers;
//    private final Long2ObjectOpenHashMap<List<EndpointLink>> netEndpoints;
//    private final AirFlowFieldSolver flowSolver;
//
//    AirDebugSnapshotUpdater(Long2ObjectOpenHashMap<AirNetwork> networks,
//                            Long2ObjectOpenHashMap<LongSet> netMembers,
//                            Long2ObjectOpenHashMap<List<EndpointLink>> netEndpoints,
//                            AirFlowFieldSolver flowSolver) {
//        this.networks = networks;
//        this.netMembers = netMembers;
//        this.netEndpoints = netEndpoints;
//        this.flowSolver = flowSolver;
//    }
//
//    void update(Level level) {
//        for (var e : networks.long2ObjectEntrySet()) {
//            long id = e.getLongKey();
//            AirNetwork net = e.getValue();
//            if (net == null) continue;
//
//            var view = net.view();
//
//            LongSet members = netMembers.get(id);
//            int ductCount = (members == null) ? 0 : members.size();
//
//            List<EndpointLink> eps = netEndpoints.get(id);
//            int endpointCount = (eps == null) ? 0 : eps.size();
//
//            // Update ducts
//            if (members != null) {
//                for (LongIterator it = members.iterator(); it.hasNext(); ) {
//                    BlockPos p = BlockPos.of(it.nextLong());
//                    PneumaticDuctBlockEntity d = getDuct(level, p);
//                    if (d != null) {
//                        Direction flow = flowSolver.getItemFlowDirection(p);
//                        String items = d.getDebugDuctItemsSummary();
//                        d.setDebugSnapshot(view.pressureKpa(), view.totalVolumeLiters(), ductCount, endpointCount, flow, items);
//                    }
//                }
//            }
//
//            // Update endpoints
//            if (eps != null) {
//                for (EndpointLink link : eps) {
//                    BlockPos p = BlockPos.of(link.posLong());
//                    Direction side = Direction.values()[link.sideOrd()];
//                    BlockEntity be = level.getBlockEntity(p);
//
//                    if (be instanceof AirTankBlockEntity tank) {
//                        tank.setDebugSnapshot(view.pressureKpa(), view.totalVolumeLiters(), view.pvKpaLiters(),
//                                ductCount, endpointCount, side);
//                    } else if (be instanceof AirCompressorBlockEntity comp) {
//                        comp.setDebugSnapshot(view.pressureKpa(), view.totalVolumeLiters(), view.pvKpaLiters(),
//                                ductCount, endpointCount, side);
//                    }
//                }
//            }
//        }
//    }
//
//    private PneumaticDuctBlockEntity getDuct(Level level, BlockPos pos) {
//        BlockEntity be = level.getBlockEntity(pos);
//        return (be instanceof PneumaticDuctBlockEntity duct) ? duct : null;
//    }
//}
