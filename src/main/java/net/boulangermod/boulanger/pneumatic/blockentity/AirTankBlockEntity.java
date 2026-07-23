//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.blockentity;
//
//import net.boulangermod.boulanger.block.entity.ModBlockEntities;
//import net.boulangermod.boulanger.pneumatic.api.IAirEndpoint;
//import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.protocol.Packet;
//import net.minecraft.network.protocol.game.ClientGamePacketListener;
//import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//
//public class AirTankBlockEntity extends BlockEntity implements IAirEndpoint {
//
//    public static final double TANK_VOLUME_L = 50.0; // big buffer
//
//    private static final String NBT_AIR_NETWORK_ID         = "AirNetworkId";
//    private static final String NBT_DBG_PRESSURE_KPA       = "DbgPressureKpa";
//    private static final String NBT_DBG_TOTAL_VOLUME_L     = "DbgTotalVolumeL";
//    private static final String NBT_DBG_PV_KPA_L           = "DbgPvKpaL";
//    private static final String NBT_DBG_DUCT_COUNT         = "DbgDuctCount";
//    private static final String NBT_DBG_ENDPOINT_COUNT     = "DbgEndpointCount";
//    private static final String NBT_DBG_CONNECTED_SIDE     = "DbgConnectedSide"; // byte, -1 = none
//
//    private long airNetworkId;
//
//    // Debug snapshot values (server writes, client reads)
//    private float dbgPressureKpa = 0.0f;
//    private float dbgTotalVolumeLiters = 0.0f;
//    private float dbgPvKpaLiters = 0.0f;
//    private int dbgDuctCount = 0;
//    private int dbgEndpointCount = 0;
//    private byte dbgConnectedSide = -1;
//
//    public AirTankBlockEntity(BlockPos pos, BlockState state) {
//        super(ModBlockEntities.AIR_TANK_BE.get(), pos, state);
//    }
//
//    @Override
//    public void onLoad() {
//        super.onLoad();
//        onTopologyMaybeChanged();
//    }
//
//    /** Call from block hooks (onPlace/onRemove/neighborChanged) to rebuild connectivity. */
//    public void requestAirNetworkRebuild() {
//        if (level != null && !level.isClientSide) {
//            AirNetworkManager.get(level).enqueueTopologyChange(worldPosition);
//        }
//    }
//
//    /** Back-compat with your current AirTankBlock call site. */
//    @Deprecated(forRemoval = true)
//    public void onTopologyMaybeChanged() {
//        requestAirNetworkRebuild();
//    }
//
//    /**
//     * IMPORTANT:
//     * For now, treat every face as a valid pneumatic port.
//     *
//     * AirNetworks will only query the face that actually has a duct attached.
//     * This keeps tanks from being “net=0” just because the duct is on the “wrong” side.
//     */
//    @SuppressWarnings("unused")
//    private boolean isPortSide(Direction side) {
//        return true;
//    }
//
//    @Override
//    public double getAirVolumeLiters(Direction side) {
//        // Single internal volume regardless of which side is used.
//        return TANK_VOLUME_L;
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
//        // Avoid spamming block updates if nothing changed
//        if (approximatelyEqual(dbgPressureKpa, p)
//                && approximatelyEqual(dbgTotalVolumeLiters, v)
//                && approximatelyEqual(dbgPvKpaLiters, pv)
//                && dbgDuctCount == ductCount
//                && dbgEndpointCount == endpointCount
//                && dbgConnectedSide == side) {
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
//        markDirtyAndSync();
//    }
//
//    private static boolean approximatelyEqual(float a, float b) {
//        return Math.abs(a - b) < 0.01f;
//    }
//
//    // ---- Client-readable accessors for your action bar overlay ----
//
//    public float dbgPressureKpa() { return dbgPressureKpa; }
//    public float dbgTotalVolumeLiters() { return dbgTotalVolumeLiters; }
//    public float dbgPvKpaLiters() { return dbgPvKpaLiters; }
//    public int dbgDuctCount() { return dbgDuctCount; }
//    public int dbgEndpointCount() { return dbgEndpointCount; }
//    public Direction dbgConnectedSide() {
//        return dbgConnectedSide < 0 ? null : Direction.values()[dbgConnectedSide];
//    }
//
//    // ---- Sync plumbing (same pattern as your duct BE) ----
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
//    }
//
//    private void markDirtyAndSync() {
//        if (level == null || level.isClientSide) return;
//        setChanged();
//        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
//    }
//}
