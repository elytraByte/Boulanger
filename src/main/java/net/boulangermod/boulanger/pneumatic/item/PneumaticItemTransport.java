//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.item;
//
//import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
//import it.unimi.dsi.fastutil.longs.LongArrayList;
//import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
//import net.boulangermod.boulanger.block.pneumatic.DuctSide;
//import net.boulangermod.boulanger.block.pneumatic.OneWayValveDuctBlock;
//import net.boulangermod.boulanger.block.pneumatic.PneumaticDuctBlock;
//import net.boulangermod.boulanger.pneumatic.blockentity.PneumaticDuctBlockEntity;
//import net.boulangermod.boulanger.pneumatic.network.AirFlowFieldSolver;
//import net.boulangermod.boulanger.pneumatic.network.AirNetwork;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import net.neoforged.neoforge.capabilities.Capabilities;
//import net.neoforged.neoforge.items.IItemHandler;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.ArrayDeque;
//import java.util.Deque;
//import java.util.function.LongFunction;
//
///**
// * Thermal-style "traveling items", driven by the air flow field.
// *
// * - Items live inside ducts as PneumaticTravelingItem records.
// * - Each server tick, active ducts are processed, items move one hop along their route.
// * - Routes are found with a BFS that respects:
// *   - duct connectivity (DuctSide.CONNECTED)
// *   - blind flanges / closed valves via ductBe.isSideBlocked()
// *   - one-way valves via AirFlowFieldSolver.canTraverseFlowEdge()
// * - Item insertion is insert-only (ducts are not extractable inventories).
// */
//public final class PneumaticItemTransport {
//    // How many items per traveling entity (reduces per-tick overhead).
//    private static final int INSERT_CHUNK = 16;
//
//    // Base movement: with 101 kPa this is roughly 1 hop every ~4 ticks; with 200 kPa it approaches 1 hop/tick.
//    // You can tune this later.
//    private static final float SPEED_BASE_PER_TICK = 0.25f;
//    private static final float SPEED_AT_MAX_PER_TICK = 1.00f;
//
//    private final AirFlowFieldSolver flowSolver;
//
//    // Which ducts need item ticking this tick.
//    private final LongOpenHashSet activeDucts = new LongOpenHashSet();
//
//    public PneumaticItemTransport(AirFlowFieldSolver flowSolver) {
//        this.flowSolver = flowSolver;
//    }
//
//    public void markActive(BlockPos ductPos) {
//        activeDucts.add(ductPos.asLong());
//    }
//
//    public void clearAllActive() {
//        activeDucts.clear();
//    }
//
//    /**
//     * Insert an ItemStack into a duct from the given side (capability insert).
//     * Returns the remainder that could not be inserted.
//     */
//    public ItemStack insertFromSide(Level level,
//                                    BlockPos ductPos,
//                                    Direction fromSide,
//                                    ItemStack stack,
//                                    boolean simulate) {
//        if (stack.isEmpty()) return stack;
//        if (level.isClientSide) return stack;
//        if (!level.isLoaded(ductPos)) return stack;
//
//        BlockEntity be0 = level.getBlockEntity(ductPos);
//        if (!(be0 instanceof PneumaticDuctBlockEntity ductBe)) return stack;
//
//        // Only allow entry if this duct face is actually open/connected and not blocked.
//        if (!canEnterDuct(level, ductPos, ductBe, fromSide)) return stack;
//
//        int remaining = stack.getCount();
//        int inserted = 0;
//
//        while (remaining > 0) {
//            int take = Math.min(remaining, INSERT_CHUNK);
//            ItemStack chunk = stack.copy();
//            chunk.setCount(take);
//
//            // Find an initial route (may be null; item will retry each tick).
//            byte[] route = findRoute(level, ductPos, chunk, fromSide);
//
//            if (!simulate) {
//                PneumaticTravelingItem it = new PneumaticTravelingItem(chunk, fromSide, 0, route);
//                ductBe.addTravelingItem(it);
//                markActive(ductPos);
//            }
//
//            inserted += take;
//            remaining -= take;
//        }
//
//        ItemStack rem = stack.copy();
//        rem.setCount(stack.getCount() - inserted);
//        return rem.isEmpty() ? ItemStack.EMPTY : rem;
//    }
//
//    public void serverTick(Level level, long gameTime, LongFunction<AirNetwork> networkById) {
//        if (level.isClientSide) return;
//        if (activeDucts.isEmpty()) return;
//
//        // Snapshot & clear, so newly-activated ducts can run next tick (prevents runaway multi-hop in one tick).
//        LongArrayList work = new LongArrayList(activeDucts);
//        activeDucts.clear();
//
//        for (int i = 0; i < work.size(); i++) {
//            long posLong = work.getLong(i);
//            BlockPos pos = BlockPos.of(posLong);
//
//            BlockEntity be = level.getBlockEntity(pos);
//            if (!(be instanceof PneumaticDuctBlockEntity ductBe)) continue;
//
//            tickDuctItems(level, pos, ductBe, networkById);
//        }
//    }
//
//    private void tickDuctItems(Level level, BlockPos pos, PneumaticDuctBlockEntity ductBe, LongFunction<AirNetwork> networkById) {
//        AirNetwork net = networkById.apply(ductBe.getAirNetworkId());
//        float movePerTick = speedFor(net);
//
//        ductBe.beginItemTick();
//
//        for (PneumaticTravelingItem it : ductBe.getTravelingItemsView()) {
//            if (it.stack.isEmpty()) {
//                ductBe.removeTravelingItem(it);
//                continue;
//            }
//
//            // No pressure or no network -> no movement, but keep active so it can recover when network rebuilds.
//            if (movePerTick <= 0.0f) {
//                markActive(pos);
//                continue;
//            }
//
//            // Route missing or exhausted -> reroute from this duct.
//            if (it.route == null || it.step >= it.route.length) {
//                it.route = findRoute(level, pos, it.stack, it.cameFrom);
//                it.step = 0;
//
//                // Still no route -> stay here.
//                if (it.route == null || it.route.length == 0) {
//                    markActive(pos);
//                    continue;
//                }
//            }
//
//            // We intentionally move at most one hop per tick in MVP (simple + stable).
//            Direction out = Direction.from3DDataValue(it.route[it.step]);
//            if (!canExitDuct(level, pos, ductBe, out)) {
//                // Route became invalid (valve closed/flange). Force reroute next tick.
//                it.route = null;
//                it.step = 0;
//                markActive(pos);
//                continue;
//            }
//
//            BlockPos nextPos = pos.relative(out);
//
//            // If next is an inventory sink: try insert and delete/keep remainder.
//            IItemHandler sink = getSink(level, nextPos, out.getOpposite());
//            if (sink != null) {
//                ItemStack remaining = insertAll(sink, it.stack);
//                if (remaining.isEmpty()) {
//                    ductBe.removeTravelingItem(it);
//                } else {
//                    it.stack = remaining;
//                    // keep trying this sink next tick
//                    markActive(pos);
//                }
//                continue;
//            }
//
//            // Otherwise, must be a duct hop.
//            BlockEntity beNext = level.getBlockEntity(nextPos);
//            if (!(beNext instanceof PneumaticDuctBlockEntity nextDuct)) {
//                // Nowhere to go -> reroute next tick.
//                it.route = null;
//                it.step = 0;
//                markActive(pos);
//                continue;
//            }
//
//            // Respect directed edge constraints (one-way valve).
//            if (!flowSolver.canAirPass(level, pos, out)) {
//                it.route = null;
//                it.step = 0;
//                markActive(pos);
//                continue;
//            }
//
//            // Move item into next duct.
//            ductBe.removeTravelingItem(it);
//            it.cameFrom = out.getOpposite();
//            it.step += 1;
//
//            nextDuct.addTravelingItem(it);
//            markActive(nextPos);
//        }
//
//        ductBe.endItemTick();
//    }
//
//    private static float speedFor(@Nullable AirNetwork net) {
//        if (net == null) return 0.0f;
//
//        float kpa = (float) net.pressureKpa();
//        // Treat <= atm as "no meaningful movement"
//        if (kpa <= 101.32f) return 0.0f;
//
//        // Normalize [atm..max] -> [0..1]
//        float t = (kpa - 101.32f) / (200.0f - 101.32f);
//        t = Math.max(0.0f, Math.min(1.0f, t));
//
//        return SPEED_BASE_PER_TICK + (SPEED_AT_MAX_PER_TICK - SPEED_BASE_PER_TICK) * t;
//    }
//
//    @Nullable
//    private static IItemHandler getSink(Level level, BlockPos pos, Direction side) {
//        BlockEntity be = level.getBlockEntity(pos);
//        if (be == null) return null;
//        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, level.getBlockState(pos), be, side);
//    }
//
//    private static ItemStack insertAll(IItemHandler inv, ItemStack stack) {
//        ItemStack remaining = stack;
//        for (int slot = 0; slot < inv.getSlots(); slot++) {
//            remaining = inv.insertItem(slot, remaining, false);
//            if (remaining.isEmpty()) return ItemStack.EMPTY;
//        }
//        return remaining;
//    }
//
//    @Nullable
//    public byte[] findRoute(Level level, BlockPos startDuct, ItemStack stack, @Nullable Direction avoidFirstStep) {
//        // BFS over ducts to nearest sink.
//        Deque<Long> q = new ArrayDeque<>();
//        Long2ObjectOpenHashMap<Parent> parent = new Long2ObjectOpenHashMap<>();
//        LongOpenHashSet visited = new LongOpenHashSet();
//
//        long startLong = startDuct.asLong();
//        q.add(startLong);
//        visited.add(startLong);
//
//        while (!q.isEmpty()) {
//            long curLong = q.removeFirst();
//            BlockPos cur = BlockPos.of(curLong);
//
//            BlockEntity be = level.getBlockEntity(cur);
//            if (!(be instanceof PneumaticDuctBlockEntity ductBe)) continue;
//
//            for (Direction out : Direction.values()) {
//                if (curLong == startLong && avoidFirstStep != null && out == avoidFirstStep) {
//                    continue;
//                }
//
//                if (!canExitDuct(level, cur, ductBe, out)) continue;
//
//                BlockPos next = cur.relative(out);
//
//                // Sink?
//                IItemHandler sink = getSink(level, next, out.getOpposite());
//                if (sink != null) {
//                    // Optional: only treat as sink if it would accept at least something.
//                    ItemStack test = sink.insertItem(0, stack, true);
//                    if (test.getCount() == stack.getCount()) {
//                        // slot 0 rejected fully; still might accept in other slots, but keep it simple:
//                        // we'll accept as sink anyway and insertAll() will handle it.
//                    }
//
//                    parent.put(next.asLong(), new Parent(curLong, (byte) out.ordinal()));
//                    return buildRouteBytes(parent, startLong, next.asLong());
//                }
//
//                // Next duct?
//                BlockState toState = level.getBlockState(next);
//                if (!PneumaticDuctBlock.isDuct(toState)) continue;
//
//                // Respect directed edge constraints (one-way valve).
//                if (!flowSolver.canAirPass(level, cur, out)) continue;
//
//                long nextLong = next.asLong();
//                if (!visited.add(nextLong)) continue;
//
//                parent.put(nextLong, new Parent(curLong, (byte) out.ordinal()));
//                q.addLast(nextLong);
//            }
//        }
//
//        return null;
//    }
//
//    private static byte[] buildRouteBytes(Long2ObjectOpenHashMap<Parent> parent, long startLong, long goalLong) {
//        // Walk back from goal to start collecting directions.
//        LongArrayList dirs = new LongArrayList();
//
//        long cur = goalLong;
//        while (cur != startLong) {
//            Parent p = parent.get(cur);
//            if (p == null) return null;
//            dirs.add(p.dirOrd);
//            cur = p.prev;
//        }
//
//        // Reverse into byte[]
//        int n = dirs.size();
//        byte[] route = new byte[n];
//        for (int i = 0; i < n; i++) {
//            route[i] = (byte) dirs.getLong(n - 1 - i);
//        }
//        return route;
//    }
//
//    private static final class Parent {
//        final long prev;
//        final byte dirOrd;
//        Parent(long prev, byte dirOrd) {
//            this.prev = prev;
//            this.dirOrd = dirOrd;
//        }
//    }
//
//    /**
//     * For insertion: the duct face must be open and not blocked. We do NOT apply one-way FLOW_DIR here;
//     * one-way constraints are applied by the directed traversal rules during routing.
//     */
//    private static boolean isPortOpen(BlockState s, Direction dir) {
//        DuctSide side = s.getValue(PneumaticDuctBlock.propFor(dir));
//        return side != DuctSide.CLOSED && side != DuctSide.FLANGED;
//    }
//
//    private static boolean canEnterDuct(Level level, BlockPos ductPos, PneumaticDuctBlockEntity ductBe, Direction fromSide) {
//        BlockState s = level.getBlockState(ductPos);
//        if (!PneumaticDuctBlock.isDuct(s)) return false;
//        if (!isPortOpen(s, fromSide)) return false;
//        return !ductBe.isSideBlocked(fromSide);
//    }
//
//    private static boolean canExitDuct(Level level, BlockPos ductPos, PneumaticDuctBlockEntity ductBe, Direction dir) {
//        BlockState s = level.getBlockState(ductPos);
//        if (!PneumaticDuctBlock.isDuct(s)) return false;
//        if (!isPortOpen(s, dir)) return false;
//        return !ductBe.isSideBlocked(dir);
//    }
//
//}
