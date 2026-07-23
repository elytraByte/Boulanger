//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.blockentity;
//
//import net.boulangermod.boulanger.block.entity.ModBlockEntities;
//import net.boulangermod.boulanger.pneumatic.api.AirNetworkView;
//import net.boulangermod.boulanger.pneumatic.api.IAirEndpoint;
//import net.boulangermod.boulanger.pneumatic.network.AirConstants;
//import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.protocol.Packet;
//import net.minecraft.network.protocol.game.ClientGamePacketListener;
//import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//
//public class AirCompressorBlockEntity extends BlockEntity implements IAirEndpoint {
//
//    // Tune these freely
//    public static final double PORT_VOLUME_L = 0.10;
//    public static final double TARGET_PRESSURE_KPA = 200.0;
//    public static final double PUMP_RATE_PV_PER_TICK = 25.0;
//
//    private static final String NBT_AIR_NETWORK_ID     = "AirNetworkId";
//
//    // Debug snapshot NBT (for client overlay)
//    private static final String NBT_DBG_PRESSURE_KPA   = "DbgPressureKpa";
//    private static final String NBT_DBG_TOTAL_VOLUME_L = "DbgTotalVolumeL";
//    private static final String NBT_DBG_PV_KPA_L       = "DbgPvKpaL";
//    private static final String NBT_DBG_DUCT_COUNT     = "DbgDuctCount";
//    private static final String NBT_DBG_ENDPOINT_COUNT = "DbgEndpointCount";
//    private static final String NBT_DBG_CONNECTED_SIDE = "DbgConnectedSide";
//    private static final String NBT_DBG_POWERED        = "DbgPowered";
//    private static final String NBT_DBG_LAST_PV_ADD    = "DbgLastPvAdd";
//    private static final String NBT_DBG_TARGET_KPA     = "DbgTargetKpa";
//
//    private long airNetworkId;
//
//    // Debug snapshot (server writes, client reads)
//    private float dbgPressureKpa = 0.0f;
//    private float dbgTotalVolumeLiters = 0.0f;
//    private float dbgPvKpaLiters = 0.0f;
//    private int dbgDuctCount = 0;
//    private int dbgEndpointCount = 0;
//    private byte dbgConnectedSide = -1;
//
//    private boolean dbgPowered = false;
//    private float dbgLastPvAdd = 0.0f;
//    private float dbgTargetKpa = (float) TARGET_PRESSURE_KPA;
//
//    public AirCompressorBlockEntity(BlockPos pos, BlockState state) {
//        super(ModBlockEntities.AIR_COMPRESSOR_BE.get(), pos, state);
//    }
//
//    @Override
//    public void onLoad() {
//        super.onLoad();
//        if (level != null && !level.isClientSide) {
//            AirNetworkManager.get(level).enqueueTopologyChange(worldPosition);
//        }
//    }
//
//    /** Call from your block's neighborChanged / onPlace / onRemove hooks. */
//    public void onTopologyMaybeChanged() {
//        if (level != null && !level.isClientSide) {
//            AirNetworkManager.get(level).enqueueTopologyChange(worldPosition);
//        }
//    }
//
//    // ---- Endpoint shape ----
//
//    private boolean isPowered() {
//        // Trust the world’s redstone directly; this works even if the POWERED
//        // blockstate isn’t wired up yet.
//        Level lvl = level;
//        return lvl != null && lvl.hasNeighborSignal(worldPosition);
//    }
//    /**
//     * For now treat every face as a valid pneumatic port.
//     * AirNetworks only calls us for the face that actually has a duct attached.
//     */
//    private boolean isPortSide(Direction side) {
//        return true;
//    }
//
//    @Override
//    public double getAirVolumeLiters(Direction side) {
//        // Single internal volume regardless of which side is used
//        return PORT_VOLUME_L;
//    }
//
//    @Override
//    public double getPvAddedThisTick(AirNetworkView view, Direction side) {
//        boolean powered = isPowered();
//        dbgPowered = powered;
//        dbgTargetKpa = (float) TARGET_PRESSURE_KPA;
//
//        if (!powered) {
//            dbgLastPvAdd = 0.0f;
//            return 0.0;
//        }
//
//        double target = Math.min(TARGET_PRESSURE_KPA, AirConstants.MAX_PRESSURE_KPA);
//
//        double desiredPv = target * view.totalVolumeLiters();
//        double curPv = view.pvKpaLiters();
//        double deficit = desiredPv - curPv;
//
//        if (deficit <= 0.0) {
//            dbgLastPvAdd = 0.0f;
//            return 0.0;
//        }
//
//        double add = Math.min(PUMP_RATE_PV_PER_TICK, deficit);
//        dbgLastPvAdd = (float) add;
//        return add;
//    }
//
//    // ---- Network assignment + debug snapshots (called by AirNetworks) ----
//
//    public long getAirNetworkId() {
//        return airNetworkId;
//    }
//
//    public void setAirNetworkId(long id) {
//        if (this.airNetworkId == id) return;
//        this.airNetworkId = id;
//        markDirtyAndSync();
//    }
//
//    public void setDebugSnapshot(double pressureKpa,
//                                 double totalVolumeLiters,
//                                 double pvKpaLiters,
//                                 int ductCount,
//                                 int endpointCount,
//                                 Direction connectedSide) {
//        if (level != null && level.isClientSide) return;
//
//        float p = (float) pressureKpa;
//        float v = (float) totalVolumeLiters;
//        float pv = (float) pvKpaLiters;
//        byte side = (byte) (connectedSide == null ? -1 : connectedSide.ordinal());
//
//        boolean powered = isPowered();
//        float lastAdd = dbgLastPvAdd;                 // updated during tick
//        float target = (float) TARGET_PRESSURE_KPA;
//
//        if (approximatelyEqual(dbgPressureKpa, p)
//                && approximatelyEqual(dbgTotalVolumeLiters, v)
//                && approximatelyEqual(dbgPvKpaLiters, pv)
//                && dbgDuctCount == ductCount
//                && dbgEndpointCount == endpointCount
//                && dbgConnectedSide == side
//                && dbgPowered == powered
//                && approximatelyEqual(dbgLastPvAdd, lastAdd)
//                && approximatelyEqual(dbgTargetKpa, target)) {
//            return;
//        }
//
//        dbgPressureKpa = p;
//        dbgTotalVolumeLiters = v;
//        dbgPvKpaLiters = pv;
//        dbgDuctCount = ductCount;
//        dbgEndpointCount = endpointCount;
//        dbgConnectedSide = side;
//
//        dbgPowered = powered;
//        dbgLastPvAdd = lastAdd;
//        dbgTargetKpa = target;
//
//        markDirtyAndSync();
//    }
//
//    private static boolean approximatelyEqual(float a, float b) {
//        return Math.abs(a - b) < 0.01f;
//    }
//
//    // ---- Client-readable accessors (PneumaticClientEvents uses these) ----
//
//    public float dbgPressureKpa() { return dbgPressureKpa; }
//    public float dbgTotalVolumeLiters() { return dbgTotalVolumeLiters; }
//    public float dbgPvKpaLiters() { return dbgPvKpaLiters; }
//    public int dbgDuctCount() { return dbgDuctCount; }
//    public int dbgEndpointCount() { return dbgEndpointCount; }
//
//    public Direction dbgConnectedSide() {
//        return dbgConnectedSide < 0 ? null : Direction.values()[dbgConnectedSide];
//    }
//
//    public boolean dbgPowered() { return dbgPowered; }
//    public float dbgLastPvAdded() { return dbgLastPvAdd; }
//    public float dbgTargetKpa() { return dbgTargetKpa; }
//
//    // ---- Sync plumbing (same pattern as PneumaticDuctBlockEntity) ----
//
//    @Override
//    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
//        CompoundTag tag = new CompoundTag();
//        saveAdditional(tag, registries);
//        return tag;
//    }
//
//    @Override
//    public Packet<ClientGamePacketListener> getUpdatePacket() {
//        return ClientboundBlockEntityDataPacket.create(this);
//    }
//
//    @Override
//    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
//        super.loadAdditional(tag, registries);
//        airNetworkId = tag.getLong(NBT_AIR_NETWORK_ID);
//
//        dbgPressureKpa = tag.getFloat(NBT_DBG_PRESSURE_KPA);
//        dbgTotalVolumeLiters = tag.getFloat(NBT_DBG_TOTAL_VOLUME_L);
//        dbgPvKpaLiters = tag.getFloat(NBT_DBG_PV_KPA_L);
//        dbgDuctCount = tag.getInt(NBT_DBG_DUCT_COUNT);
//        dbgEndpointCount = tag.getInt(NBT_DBG_ENDPOINT_COUNT);
//        dbgConnectedSide = tag.getByte(NBT_DBG_CONNECTED_SIDE);
//
//        dbgPowered = tag.getBoolean(NBT_DBG_POWERED);
//        dbgLastPvAdd = tag.getFloat(NBT_DBG_LAST_PV_ADD);
//        dbgTargetKpa = tag.getFloat(NBT_DBG_TARGET_KPA);
//    }
//
//    @Override
//    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
//        super.saveAdditional(tag, registries);
//        tag.putLong(NBT_AIR_NETWORK_ID, airNetworkId);
//
//        tag.putFloat(NBT_DBG_PRESSURE_KPA, dbgPressureKpa);
//        tag.putFloat(NBT_DBG_TOTAL_VOLUME_L, dbgTotalVolumeLiters);
//        tag.putFloat(NBT_DBG_PV_KPA_L, dbgPvKpaLiters);
//        tag.putInt(NBT_DBG_DUCT_COUNT, dbgDuctCount);
//        tag.putInt(NBT_DBG_ENDPOINT_COUNT, dbgEndpointCount);
//        tag.putByte(NBT_DBG_CONNECTED_SIDE, dbgConnectedSide);
//
//        tag.putBoolean(NBT_DBG_POWERED, dbgPowered);
//        tag.putFloat(NBT_DBG_LAST_PV_ADD, dbgLastPvAdd);
//        tag.putFloat(NBT_DBG_TARGET_KPA, dbgTargetKpa);
//    }
//
//    private void markDirtyAndSync() {
//        if (level == null || level.isClientSide) return;
//        setChanged();
//        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
//    }
//}
