package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.WoodGasFeedThroughBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Feedthrough BE that exposes a stable proxy on EVERY face.
 * Each face forwards operations to the opposite neighbor's handler (if present).
 * Also stores an optional "cover" BlockState for camo rendering.
 */
public class WoodGasFeedThroughBlockEntity extends BlockEntity {

    // Proxies: one per face
    private final Map<Direction, IFluidHandler> proxies = new EnumMap<>(Direction.class);

    // Camo (nullable → fallback will be used)
    private @Nullable BlockState cover;

    public WoodGasFeedThroughBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WALL_FEED_THROUGH_BE.get(), pos, state);
    }

    /* ==================== Capability exposure ==================== */

    /** Called from ModCapabilities registration. Always non-null on any face. */
    public @Nullable IFluidHandler getHandlerFor(@Nullable Direction side) {
        if (side == null) return null;
        return proxies.computeIfAbsent(side, s -> new ThroughProxy(this, s));
    }

    /** Resolve the real handler across the block for the given face (opposite neighbor). */
    private @Nullable IFluidHandler resolveRemote(Direction exposedSide) {
        Level lvl = getLevel();
        if (lvl == null) return null;

        BlockPos otherPos   = worldPosition.relative(exposedSide.getOpposite());
        BlockState otherSt  = lvl.getBlockState(otherPos);
        BlockEntity otherBE = lvl.getBlockEntity(otherPos);
        Direction neighborFace = exposedSide; // neighbor's face toward us

        return lvl.getCapability(
                Capabilities.FluidHandler.BLOCK,
                otherPos, otherSt, otherBE, neighborFace
        );
    }

    /** Proxy that forwards to the opposite neighbor; no-ops if absent. */
    private static final class ThroughProxy implements IFluidHandler {
        private final WoodGasFeedThroughBlockEntity be;
        private final Direction side;

        ThroughProxy(WoodGasFeedThroughBlockEntity be, Direction side) {
            this.be = be; this.side = side;
        }

        private @Nullable IFluidHandler remote() { return be.resolveRemote(side); }

        @Override public int getTanks() { IFluidHandler r = remote(); return (r != null) ? r.getTanks() : 1; }
        @Override public FluidStack getFluidInTank(int tank) { IFluidHandler r = remote(); return (r != null) ? r.getFluidInTank(tank) : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { IFluidHandler r = remote(); return (r != null) ? r.getTankCapacity(tank) : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { IFluidHandler r = remote(); return (r != null) && r.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { IFluidHandler r = remote(); return (r != null) ? r.fill(resource, action) : 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { IFluidHandler r = remote(); return (r != null) ? r.drain(resource, action) : FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { IFluidHandler r = remote(); return (r != null) ? r.drain(maxDrain, action) : FluidStack.EMPTY; }
    }

    /* ==================== Camo API & sync ==================== */

    public void setCover(@Nullable BlockState cover) {
        this.cover = cover;
        setChangedAndNotify();
    }

    public BlockState getCoverOrFallback() {
        return cover != null ? cover : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    }

    private void setChangedAndNotify() {
        setChanged();

        if (level == null) return;

        // force a re-render + light recalc on clients
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

        if (level instanceof ServerLevel sl) {
            ClientboundBlockEntityDataPacket pkt = ClientboundBlockEntityDataPacket.create(this);
            if (pkt != null) {
                ChunkPos cp = new ChunkPos(worldPosition);
                for (ServerPlayer p : sl.getChunkSource().chunkMap.getPlayers(cp, false)) {
                    p.connection.send(pkt);
                }
            }
            // (optional) also poke chunk system that a block changed
            // sl.getChunkSource().blockChanged(worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (cover != null) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(cover.getBlock());
            if (id != null) tag.putString("CoverBlock", id.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("CoverBlock")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("CoverBlock"));
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                cover = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
            } else {
                cover = null;
            }
        } else {
            cover = null;
        }
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt,
                                       HolderLookup.Provider regs) { loadAdditional(pkt.getTag(), regs); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs); saveAdditional(tag, regs); return tag;
    }

}
