package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Cable network with leader-orchestrated, network-wide round-robin distribution.
 */
public class EnergyCableBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Tunables
    private static final int CAP            = 100; // internal buffer size
    private static final int TICK_TRANSFER  = 500;    // total IO per *cable* per tick (used to size budgets)
    private static final int PUSH_STEP      = 50;     // per-sink chunk
    private static final int EQ_MARGIN      = 10;     // don't equalize if diff <= this (prevents jitter)
    private static final int EQ_ROUNDS      = 3;      // small number of equalization passes per tick
    private static final int MAX_BFS_NODES  = 1024;   // safety cap for huge networks

    /** Local buffer. */
    private final ModEnergyStorage buffer = new ModEnergyStorage(CAP, CAP, CAP) {
        @Override protected void onEnergyChanged() { setChanged(); }
    };

    /** Per-side capability (receive-only) + unsided. */
    private final IEnergyStorage[] sideCaps = new IEnergyStorage[6];
    private final IEnergyStorage unsidedPort = new ReceiveOnlyPort(this);

    // For logging
    private int lastCombinedMask = Integer.MIN_VALUE;
    private int lastCableMask    = 0;
    private int lastDeviceMask   = 0;

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE_BE.get(), pos, state);
        for (Direction d : Direction.values()) {
            sideCaps[d.ordinal()] = new ReceiveOnlyPort(this);
        }
    }

    /** Exposed via capability registration: receive-only. */
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return (side == null) ? unsidedPort : sideCaps[side.ordinal()];
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LOGGER.info("[EnergyCable] loaded @ {} in {}", worldPosition, level.dimension().location());
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            LOGGER.info("[EnergyCable] removed @ {}", worldPosition);
        }
        super.setRemoved();
    }

    // ────────────────────────────── TICK ──────────────────────────────
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos,
                                                    BlockState st, EnergyCableBlockEntity be) {
        if (level.isClientSide()) return;

        // 0) Quick neighbor snapshot → logs/debug only
        int cableMask  = 0;
        int deviceMask = 0;
        for (Direction dir : Direction.values()) {
            BlockPos np = pos.relative(dir);
            BlockEntity nbe = level.getBlockEntity(np);
            if (nbe instanceof EnergyCableBlockEntity) {
                cableMask |= (1 << dir.ordinal());
            } else {
                IEnergyStorage neigh = level.getCapability(Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
                if (neigh != null) deviceMask |= (1 << dir.ordinal());
            }
        }
        int combined = cableMask | deviceMask;
        if (combined != be.lastCombinedMask) {
            be.lastCombinedMask = combined;
            be.lastCableMask    = cableMask;
            be.lastDeviceMask   = deviceMask;
            LOGGER.info("[EnergyCable] {} connections @ {} → total={}, cables={}({}), devices={}({})",
                    (combined == 0 ? "no" : "updated"),
                    pos,
                    Integer.bitCount(combined),
                    Integer.bitCount(cableMask),  dirsToString(cableMask),
                    Integer.bitCount(deviceMask), dirsToString(deviceMask));
        }

        // 1) Discover network & elect leader
        Network net = discoverNetwork(level, pos);
        if (net.cables.isEmpty()) { be.setChanged(); return; }

        BlockPos leaderPos = net.leader;
        boolean iAmLeader  = be.worldPosition.equals(leaderPos);

        // 2) Equalize locally for everyone (cheap pressure smoothing)
        int ioBudget = Math.min(be.buffer.getEnergyStored(), TICK_TRANSFER);
        equalizeWithNeighborCables(level, pos, be, Math.min(ioBudget, TICK_TRANSFER));

        // 3) Only the leader performs network I/O (pull from sources + round-robin push to sinks)
        if (iAmLeader) {
            orchestrateNetworkIO(level, net);
        }

        be.setChanged();
    }

    // ─────────────────────────── Network Orchestration ───────────────────────────

    private static void orchestrateNetworkIO(Level level, Network net) {
        if (net.sinks.isEmpty() && net.sources.isEmpty()) return;

        // 3a) Pull from sources into adjacent cable buffers
        int networkPullBudget = TICK_TRANSFER * Math.max(1, net.cables.size());
        if (!net.sources.isEmpty() && networkPullBudget > 0) {
            for (Endpoint srcEp : rotatedEndpoints(level, net.sources, net.leader)) {
                if (networkPullBudget <= 0) break;

                IEnergyStorage src = level.getCapability(Capabilities.EnergyStorage.BLOCK, srcEp.devicePos, srcEp.faceTowardDevice);
                if (src == null) continue;

                EnergyCableBlockEntity cable = net.cableMap.get(srcEp.cablePos);
                if (cable == null) continue;

                int space = cable.buffer.getMaxEnergyStored() - cable.buffer.getEnergyStored();
                if (space <= 0) continue;

                int step = Math.min(PUSH_STEP, Math.min(space, networkPullBudget));
                int canExtract = src.extractEnergy(step, true);
                if (canExtract <= 0) continue;

                int accepted = cable.buffer.receiveEnergy(canExtract, false);
                if (accepted > 0) {
                    src.extractEnergy(accepted, false);
                    networkPullBudget -= accepted;
                }
            }
        }

        // 3b) Round-robin push to sinks across the whole network
        if (net.sinks.isEmpty()) return;

        // Total available across cables determines max network throughput this tick
        int totalEnergy = net.cables.stream()
                .map(net.cableMap::get)
                .filter(Objects::nonNull)
                .mapToInt(c -> c.buffer.getEnergyStored())
                .sum();

        int networkPushBudget = Math.min(totalEnergy, TICK_TRANSFER * Math.max(1, net.cables.size()));
        if (networkPushBudget <= 0) return;

        // Start index rotates with time for fairness
        int start = Math.floorMod((int)(level.getGameTime() + net.seed), net.sinks.size());
        int moved = 0;

        for (int i = 0; i < net.sinks.size() && networkPushBudget > 0; i++) {
            Endpoint sinkEp = net.sinks.get((start + i) % net.sinks.size());
            IEnergyStorage sink = level.getCapability(Capabilities.EnergyStorage.BLOCK, sinkEp.devicePos, sinkEp.faceTowardDevice);
            if (sink == null) continue;

            EnergyCableBlockEntity adjCable = net.cableMap.get(sinkEp.cablePos);
            if (adjCable == null) continue;

            int step = Math.min(PUSH_STEP, networkPushBudget);

            // Ensure adjacent cable has 'step' energy available; if not, siphon from peers
            int have = adjCable.buffer.extractEnergy(step, true);
            if (have < step) {
                int need = step - have;
                int gained = siphonFromPeers(net, sinkEp.cablePos, need);
                // gained energy went into the adjacent cable buffer
            }

            int available = adjCable.buffer.extractEnergy(step, true);
            if (available <= 0) continue;

            int accepted = sink.receiveEnergy(available, true);
            if (accepted <= 0) continue;

            int sent = sink.receiveEnergy(accepted, false);
            if (sent > 0) {
                adjCable.buffer.extractEnergy(sent, false);
                moved += sent;
                networkPushBudget -= sent;
            }
        }

        // (Optional) debug trace
        // LOGGER.debug("[EnergyCable] Leader @ {} moved {} FE to sinks (net size={}, sinks={})",
        //         net.leader, moved, net.cables.size(), net.sinks.size());
    }

    /** Move up to 'amount' FE from other cables' buffers into the target cable's buffer. */
    private static int siphonFromPeers(Network net, BlockPos targetCable, int amount) {
        EnergyCableBlockEntity target = net.cableMap.get(targetCable);
        if (target == null || amount <= 0) return 0;

        int moved = 0;
        // Iterate donors in a rotated order for fairness
        List<BlockPos> donors = rotatedCableList(net);
        for (BlockPos donorPos : donors) {
            if (moved >= amount) break;
            if (donorPos.equals(targetCable)) continue;

            EnergyCableBlockEntity donor = net.cableMap.get(donorPos);
            if (donor == null) continue;

            int room = target.buffer.getMaxEnergyStored() - target.buffer.getEnergyStored();
            if (room <= 0) break;

            int step = Math.min(PUSH_STEP, Math.min(amount - moved, room));
            int extracted = donor.buffer.extractEnergy(step, false);
            if (extracted > 0) {
                int accepted = target.buffer.receiveEnergy(extracted, false);
                if (accepted < extracted) {
                    // put back any unaccepted (shouldn't happen with internal buffers)
                    donor.buffer.receiveEnergy(extracted - accepted, false);
                } else {
                    moved += accepted;
                }
            }
        }
        return moved;
    }

    // ─────────────────────── Equalize with neighbor cables ───────────────────────
    private static int equalizeWithNeighborCables(Level level, BlockPos pos, EnergyCableBlockEntity be, int budget) {
        if (budget <= 0) return 0;

        int movedTotal = 0;
        Direction[] order = rotatedDirs(level, pos);

        for (int round = 0; round < EQ_ROUNDS && budget > 0; round++) {
            boolean progress = false;

            for (Direction dir : order) {
                if (budget <= 0) break;

                BlockPos np = pos.relative(dir);
                BlockEntity nbe = level.getBlockEntity(np);
                if (!(nbe instanceof EnergyCableBlockEntity other)) continue;

                int mine   = be.buffer.getEnergyStored();
                int theirs = other.buffer.getEnergyStored();
                int diff   = mine - theirs;

                if (diff <= EQ_MARGIN) continue;

                int step = Math.min(PUSH_STEP, Math.min(diff / 2, budget));
                if (step <= 0) continue;

                int extracted = be.buffer.extractEnergy(step, false);
                if (extracted <= 0) continue;

                int accepted = other.buffer.receiveEnergy(extracted, false);
                if (accepted < extracted) {
                    be.buffer.receiveEnergy(extracted - accepted, false);
                } else {
                    budget     -= accepted;
                    movedTotal += accepted;
                    progress = true;
                }
            }

            if (!progress) break;
        }

        return movedTotal;
    }

    // ───────────────────────────── Network discovery ─────────────────────────────

    private static final class Endpoint {
        final BlockPos devicePos;         // device block position
        final Direction faceTowardDevice; // the face on device that faces the cable (pass directly to capability lookup)
        final BlockPos cablePos;          // the adjacent cable position (closest cable to this device)

        Endpoint(BlockPos devicePos, Direction faceTowardDevice, BlockPos cablePos) {
            this.devicePos = devicePos;
            this.faceTowardDevice = faceTowardDevice;
            this.cablePos = cablePos;
        }
    }

    private static final class Network {
        final BlockPos leader;
        final List<BlockPos> cables;             // all cable positions in this network
        final Map<BlockPos, EnergyCableBlockEntity> cableMap; // position -> BE
        final List<Endpoint> sinks;              // devices that can RECEIVE
        final List<Endpoint> sources;            // devices that can EXTRACT
        final long seed;                         // for stable rotation, derived from leader

        Network(BlockPos leader,
                List<BlockPos> cables,
                Map<BlockPos, EnergyCableBlockEntity> cableMap,
                List<Endpoint> sinks,
                List<Endpoint> sources) {
            this.leader = leader;
            this.cables = cables;
            this.cableMap = cableMap;
            this.sinks = sinks;
            this.sources = sources;
            this.seed = leader.asLong();
        }
    }

    private static Network discoverNetwork(Level level, BlockPos start) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();
        ArrayList<BlockPos> cables = new ArrayList<>();
        HashMap<BlockPos, EnergyCableBlockEntity> cableMap = new HashMap<>();
        ArrayList<Endpoint> sinks = new ArrayList<>();
        ArrayList<Endpoint> sources = new ArrayList<>();

        q.add(start);
        visited.add(start);

        int nodes = 0;
        while (!q.isEmpty() && nodes < MAX_BFS_NODES) {
            BlockPos p = q.pollFirst();
            nodes++;

            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof EnergyCableBlockEntity cable)) continue;

            cables.add(p);
            cableMap.put(p, cable);

            for (Direction d : Direction.values()) {
                BlockPos np = p.relative(d);
                BlockEntity nbe = level.getBlockEntity(np);
                if (nbe instanceof EnergyCableBlockEntity) {
                    if (visited.add(np)) q.addLast(np);
                } else {
                    // Device endpoint?
                    IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, np, d.getOpposite());
                    if (cap == null) continue;

                    boolean canReceive = cap.receiveEnergy(1, true) > 0;
                    boolean canExtract = cap.extractEnergy(1, true) > 0;

                    if (canReceive) sinks.add(new Endpoint(np, d.getOpposite(), p));
                    if (canExtract) sources.add(new Endpoint(np, d.getOpposite(), p));
                }
            }
        }

        if (nodes >= MAX_BFS_NODES) {
            LOGGER.warn("[EnergyCable] BFS hit MAX_NODES={} starting @ {}. Network truncated for safety.", MAX_BFS_NODES, start);
        }

        // Elect leader = lowest position for determinism
        BlockPos leader = cables.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElse(start);
        return new Network(leader, cables, cableMap, sinks, sources);
    }

    // ───────────────────────────── Utilities ─────────────────────────────

    private static Direction[] rotatedDirs(Level level, BlockPos pos) {
        Direction[] base = Direction.values(); // 6
        Direction[] out  = new Direction[base.length];
        int off = Math.floorMod(level.getGameTime() + pos.asLong(), base.length);
        for (int i = 0; i < base.length; i++) out[i] = base[Math.floorMod(i + off, base.length)];
        return out;
    }

    private static List<Endpoint> rotatedEndpoints(Level level, List<Endpoint> list, BlockPos seedPos) {
        if (list.isEmpty()) return list;
        int off = Math.floorMod((int)(level.getGameTime() + seedPos.asLong()), list.size());
        if (off == 0) return list;
        ArrayList<Endpoint> rot = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) rot.add(list.get((i + off) % list.size()));
        return rot;
    }

    private static List<BlockPos> rotatedCableList(Network net) {
        if (net.cables.isEmpty()) return net.cables;
        int off = Math.floorMod((int)(net.seed), net.cables.size());
        if (off == 0) return net.cables;
        ArrayList<BlockPos> rot = new ArrayList<>(net.cables.size());
        for (int i = 0; i < net.cables.size(); i++) rot.add(net.cables.get((i + off) % net.cables.size()));
        return rot;
    }

    private static String dirsToString(int mask) {
        if (mask == 0) return "-";
        StringBuilder sb = new StringBuilder();
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.ordinal())) != 0) {
                switch (d) {
                    case NORTH -> sb.append('N');
                    case EAST  -> sb.append('E');
                    case SOUTH -> sb.append('S');
                    case WEST  -> sb.append('W');
                    case UP    -> sb.append('U');
                    case DOWN  -> sb.append('D');
                }
            }
        }
        return sb.toString();
    }

    // ─────────────────────── Capability port (receive-only) ───────────────────────
    /** Neighbors can insert into cables, but cannot extract. Cables push/equalize on their tick. */
    private static final class ReceiveOnlyPort implements IEnergyStorage {
        private final EnergyCableBlockEntity be;
        ReceiveOnlyPort(EnergyCableBlockEntity be) { this.be = be; }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            int step = Math.min(maxReceive, TICK_TRANSFER);
            return be.buffer.receiveEnergy(step, simulate);
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; } // push-only network
        @Override public int getEnergyStored()    { return be.buffer.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return be.buffer.getMaxEnergyStored(); }
        @Override public boolean canExtract()     { return false; }
        @Override public boolean canReceive()     { return true; }
    }
}
