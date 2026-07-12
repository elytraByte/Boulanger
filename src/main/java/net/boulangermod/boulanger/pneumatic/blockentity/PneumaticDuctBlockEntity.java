package net.boulangermod.boulanger.pneumatic.blockentity;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.pneumatic.DuctSide;
import net.boulangermod.boulanger.block.pneumatic.PneumaticDuctBlock;
import net.boulangermod.boulanger.pneumatic.item.PneumaticTravelingItem;
import net.boulangermod.boulanger.pneumatic.network.AirConstants;
import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PneumaticDuctBlockEntity extends BlockEntity {

    private long airNetworkId = 0L;

    // For block-side blocking (blind flange / valve closed)
    private int blockedMask = 0;

    // ===== Debug snapshot fields (server writes, client reads) =====
    public long dbgNetId = -1;
    public double dbgPressureKpa = 0;
    public double dbgTotalVolumeLiters = 0;
    public int dbgDuctCount = 0;
    public int dbgEndpointCount = 0;

    // Directed flow + items in duct (overlay)
    public byte dbgFlowDirOrd = DBG_FLOW_NONE;
    public String dbgItemsSummary = "";

    private static final byte DBG_FLOW_NONE = 127;

    private static final String NBT_DBG_NET_ID = "DbgNetId";
    private static final String NBT_DBG_PRESSURE_KPA = "DbgP";
    private static final String NBT_DBG_TOTAL_VOLUME_L = "DbgV";
    private static final String NBT_DBG_DUCT_COUNT = "DbgD";
    private static final String NBT_DBG_ENDPOINT_COUNT = "DbgE";
    private static final String NBT_DBG_FLOW_DIR = "DbgFlowDir";
    private static final String NBT_DBG_ITEMS_SUMMARY = "DbgItems";
    private static final String NBT_AIR_NET_ID = "AirNetId";

    // ===== Traveling items =====
    private final List<PneumaticTravelingItem> travelingItems = new ArrayList<>();
    private final List<PneumaticTravelingItem> itemsToAdd = new ArrayList<>();
    private final List<PneumaticTravelingItem> itemsToRemove = new ArrayList<>();

    // Insert-only handler (per side)
    private final Long2ObjectOpenHashMap<IItemHandler> sideHandlers = new Long2ObjectOpenHashMap<>();

    public PneumaticDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_DUCT_BE.get(), pos, state);
    }

    /**
     * Server-side: writes the debug snapshot fields that the client overlay reads.
     * Call this from AirNetworks after you compute the per-duct snapshot.
     */
    public void setDebugSnapshot(double pressureKpa,
                                 double totalVolumeLiters,
                                 int ductCount,
                                 int endpointCount,
                                 Direction flowDir,
                                 String itemsSummary) {

        // Optional: round to reduce packet spam a bit
        double p = Math.round(pressureKpa * 10.0D) / 10.0D;         // 0.1 kPa
        double v = Math.round(totalVolumeLiters * 100.0D) / 100.0D; // 0.01 L

        long netId = this.airNetworkId;
        byte flowOrd = (flowDir == null) ? DBG_FLOW_NONE : (byte) flowDir.ordinal();
        String items = (itemsSummary == null) ? "" : itemsSummary;

        boolean changed = false;

        if (this.dbgNetId != netId) { this.dbgNetId = netId; changed = true; }
        if (this.dbgPressureKpa != p) { this.dbgPressureKpa = p; changed = true; }
        if (this.dbgTotalVolumeLiters != v) { this.dbgTotalVolumeLiters = v; changed = true; }
        if (this.dbgDuctCount != ductCount) { this.dbgDuctCount = ductCount; changed = true; }
        if (this.dbgEndpointCount != endpointCount) { this.dbgEndpointCount = endpointCount; changed = true; }
        if (this.dbgFlowDirOrd != flowOrd) { this.dbgFlowDirOrd = flowOrd; changed = true; }
        if (!this.dbgItemsSummary.equals(items)) { this.dbgItemsSummary = items; changed = true; }

        if (!changed) return;

        setChanged();

        // Push to client so overlay sees fresh values
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }


    public long getAirNetworkId() {
        return airNetworkId;
    }

    // === Debug accessors used by PneumaticClientEvents ===
    public long dbgNetId() {
        return dbgNetId;
    }

    public double dbgPressureKpa() {
        return dbgPressureKpa;
    }

    public double dbgTotalVolumeLiters() {
        return dbgTotalVolumeLiters;
    }

    public int dbgDuctCount() {
        return dbgDuctCount;
    }

    public int dbgEndpointCount() {
        return dbgEndpointCount;
    }

    private static final String[] DIR_SHORT = {"D","U","N","S","W","E"};

    public String blockedSummary() {
        if (blockedMask == 0) return "blk=-";
        StringBuilder sb = new StringBuilder("blk=");
        boolean first = true;
        for (Direction d : Direction.values()) {
            if (!isSideBlocked(d)) continue;
            if (!first) sb.append(',');
            sb.append(DIR_SHORT[d.ordinal()]);
            first = false;
        }
        return sb.toString();
    }

    public void setAirNetworkId(long id) {
        this.airNetworkId = id;
    }

    public void notifyAirTopologyChanged() {
        if (level == null || level.isClientSide) return;
        AirNetworkManager.get(level).enqueueTopologyChange(worldPosition);
    }

    // === Blocking mask helpers ===
    public boolean isSideBlocked(Direction side) {
        return (blockedMask & (1 << side.ordinal())) != 0;
    }

    public void setSideBlocked(Direction side, boolean blocked) {
        int bit = 1 << side.ordinal();
        if (blocked) blockedMask |= bit;
        else blockedMask &= ~bit;
        setChanged();
    }

    // === Item traveling API ===
    public void addTravelingItem(PneumaticTravelingItem it) {
        if (it == null || it.stack.isEmpty()) return;
        itemsToAdd.add(it);
        setChanged();
    }

    public void removeTravelingItem(PneumaticTravelingItem it) {
        if (it == null) return;
        itemsToRemove.add(it);
        setChanged();
    }

    public List<PneumaticTravelingItem> getTravelingItemsView() {
        return travelingItems;
    }

    public void beginItemTick() {
        if (!itemsToAdd.isEmpty()) {
            travelingItems.addAll(itemsToAdd);
            itemsToAdd.clear();
        }
        itemsToRemove.clear();
    }

    public void endItemTick() {
        if (!itemsToRemove.isEmpty()) {
            travelingItems.removeAll(itemsToRemove);
            itemsToRemove.clear();
        }
    }

    // Debug string for overlay: "ItemName xN | ..."
    public String getDebugDuctItemsSummary() {
        if (travelingItems.isEmpty() && itemsToAdd.isEmpty()) return "-";

        java.util.LinkedHashMap<String, Integer> counts = new java.util.LinkedHashMap<>();

        for (PneumaticTravelingItem it : travelingItems) {
            if (it.stack.isEmpty()) continue;
            String key = it.stack.getHoverName().getString();
            counts.put(key, counts.getOrDefault(key, 0) + it.stack.getCount());
            if (counts.size() >= 3) break;
        }

        if (counts.size() < 3) {
            for (PneumaticTravelingItem it : itemsToAdd) {
                if (it.stack.isEmpty()) continue;
                String key = it.stack.getHoverName().getString();
                counts.put(key, counts.getOrDefault(key, 0) + it.stack.getCount());
                if (counts.size() >= 3) break;
            }
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (var e : counts.entrySet()) {
            if (i++ > 0) sb.append(" | ");
            sb.append(e.getKey()).append(" x").append(e.getValue());
        }

        int shown = counts.size();
        int total = travelingItems.size() + itemsToAdd.size();
        int extra = total - shown;
        if (extra > 0) sb.append(" | +").append(extra).append(" more");

        return sb.toString();
    }

    // === Capability hook target ===
    public IItemHandler getItemHandler(Direction side) {
        if (level == null || level.isClientSide) return null;
        if (side == null) return null;

        long key = side.ordinal();
        IItemHandler cached = sideHandlers.get(key);
        if (cached != null) return cached;

        IItemHandler created = new InsertOnlyHandler(this, side);
        sideHandlers.put(key, created);
        return created;
    }

    // Insert-only implementation: no extraction.
    private static final class InsertOnlyHandler implements IItemHandler {
        private final PneumaticDuctBlockEntity duct;
        private final Direction side;

        private InsertOnlyHandler(PneumaticDuctBlockEntity duct, Direction side) {
            this.duct = duct;
            this.side = side;
        }

        @Override public int getSlots() { return 1; }

        @Override public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        @Override public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            if (duct.level == null || duct.level.isClientSide) return stack;

            return AirNetworkManager.get(duct.level).itemTransport().insertFromSide(duct.level, duct.worldPosition, side, stack, simulate);
        }

        @Override public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) { return 64; }

        @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) { return true; }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PneumaticDuctBlockEntity be) {
        // Client-only visual tick (leak particles / later: item animation)
        if (!level.isClientSide) return;

        // Only emit when above atmospheric
        double kpa = be.dbgPressureKpa;
        if (kpa <= AirConstants.ATM_KPA + 0.25f) return;

        // Scale intensity/speed by overpressure (0..1)
        double atm = (double) AirConstants.ATM_KPA;
        double max = (double) AirConstants.MAX_PRESSURE_KPA;

        double over = kpa - atm;
        double denom = max - atm;
        double t = denom <= 0f ? 0f : Math.min(1f, Math.max(0f, over / denom));

        // Leak = OPEN face into AIR that isn't blocked
        for (Direction dir : Direction.values()) {
            DuctSide side = state.getValue(PneumaticDuctBlock.propFor(dir));
            if (side != DuctSide.OPEN) continue;
            if (be.isSideBlocked(dir)) continue;

            // Only leak visually if the adjacent block is actually air
            if (!level.getBlockState(pos.relative(dir)).isAir()) continue;

            // 1..3 particles per tick per leaking side
            int count = 1 + (int) (t * 2.0f);

            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            // Spawn slightly outside the face
            double ox = dir.getStepX() * 0.55;
            double oy = dir.getStepY() * 0.55;
            double oz = dir.getStepZ() * 0.55;

            // Outward speed: low pressure = linger, high pressure = shoot
            double speed = 0.02 + 0.12 * t;

            for (int i = 0; i < count; i++) {
                // jitter perpendicular to the face normal
                double j1 = (level.random.nextDouble() - 0.5) * 0.25;
                double j2 = (level.random.nextDouble() - 0.5) * 0.25;

                double px = cx + ox;
                double py = cy + oy;
                double pz = cz + oz;

                switch (dir.getAxis()) {
                    case X -> { py += j1; pz += j2; }
                    case Y -> { px += j1; pz += j2; }
                    case Z -> { px += j1; py += j2; }
                }

                double vx = dir.getStepX() * speed + level.random.nextGaussian() * 0.005;
                double vy = dir.getStepY() * speed + level.random.nextGaussian() * 0.005;
                double vz = dir.getStepZ() * speed + level.random.nextGaussian() * 0.005;

                level.addParticle(ParticleTypes.CLOUD, px, py, pz, vx, vy, vz);
            }
        }
    }


    // ===== NBT =====

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        blockedMask = tag.getInt("BlkMask");

        if (tag.contains(NBT_AIR_NET_ID)) {
            airNetworkId = tag.getLong(NBT_AIR_NET_ID);
        }

        // Debug snapshot values for client overlay (optional)
        dbgNetId = tag.getLong(NBT_DBG_NET_ID);
        dbgPressureKpa = tag.getDouble(NBT_DBG_PRESSURE_KPA);
        dbgTotalVolumeLiters = tag.getDouble(NBT_DBG_TOTAL_VOLUME_L);
        dbgDuctCount = tag.getInt(NBT_DBG_DUCT_COUNT);
        dbgEndpointCount = tag.getInt(NBT_DBG_ENDPOINT_COUNT);
        dbgFlowDirOrd = tag.getByte(NBT_DBG_FLOW_DIR);
        dbgItemsSummary = tag.contains(NBT_DBG_ITEMS_SUMMARY) ? tag.getString(NBT_DBG_ITEMS_SUMMARY) : "";

        // Traveling items (server-side state)
        travelingItems.clear();
        itemsToAdd.clear();
        itemsToRemove.clear();
        if (tag.contains("TravItems", net.minecraft.nbt.Tag.TAG_LIST)) {
            var list = tag.getList("TravItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itTag = list.getCompound(i);
                PneumaticTravelingItem it = PneumaticTravelingItem.load(registries, itTag);
                if (!it.stack.isEmpty()) travelingItems.add(it);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putInt("BlkMask", blockedMask);
        tag.putLong(NBT_AIR_NET_ID, airNetworkId);

        // Traveling items (server-side state)
        if (!travelingItems.isEmpty() || !itemsToAdd.isEmpty()) {
            var list = new net.minecraft.nbt.ListTag();
            for (PneumaticTravelingItem it : travelingItems) {
                list.add(it.save(registries));
            }
            for (PneumaticTravelingItem it : itemsToAdd) {
                list.add(it.save(registries));
            }
            if (!list.isEmpty()) {
                tag.put("TravItems", list);
            }
        }

        // Debug snapshot values for client overlay (optional)
        tag.putLong(NBT_DBG_NET_ID, dbgNetId);
        tag.putDouble(NBT_DBG_PRESSURE_KPA, dbgPressureKpa);
        tag.putDouble(NBT_DBG_TOTAL_VOLUME_L, dbgTotalVolumeLiters);
        tag.putInt(NBT_DBG_DUCT_COUNT, dbgDuctCount);
        tag.putInt(NBT_DBG_ENDPOINT_COUNT, dbgEndpointCount);

        // Debug: directed flow + in-duct items summary
        tag.putByte(NBT_DBG_FLOW_DIR, dbgFlowDirOrd);
        tag.putString(NBT_DBG_ITEMS_SUMMARY, dbgItemsSummary);
    }
}