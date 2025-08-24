package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasValveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class WoodGasValveBlock extends BaseEntityBlock implements EntityBlock {

    public static final MapCodec<WoodGasValveBlock> CODEC = simpleCodec(WoodGasValveBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    // Side booleans used by your multipart blockstate to render pipe arms
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    public WoodGasValveBlock(Properties props) {
        super(props
                .noOcclusion()
                .isSuffocating((s,g,p) -> false)
                .isViewBlocking((s,g,p) -> false));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(OPEN, Boolean.TRUE)
                .setValue(NORTH, Boolean.FALSE)
                .setValue(SOUTH, Boolean.FALSE)
                .setValue(EAST,  Boolean.FALSE)
                .setValue(WEST,  Boolean.FALSE)
                .setValue(UP,    Boolean.FALSE)
                .setValue(DOWN,  Boolean.FALSE));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override public MapCodec<? extends WoodGasValveBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING, OPEN, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();

        BlockState placed = this.defaultBlockState()
                .setValue(FACING, face)
                .setValue(OPEN, true);

        // Initialize the side-connection booleans
        return recalcAllConnections(level, pos, placed);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        state = state.setValue(FACING, rot.rotate(state.getValue(FACING)));
        // Side flags rotate automatically through updateShape on neighbors; keep as-is.
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                           BlockPos pos, Player player, InteractionHand hand,
                                           BlockHitResult hit) {
        // Only toggle on *sneak* right-click; otherwise let normal item use proceed
        if (!player.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            boolean nowOpen = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, nowOpen), net.minecraft.world.level.block.Block.UPDATE_ALL);

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WoodGasValveBlockEntity valve) {
                valve.setDirtyAndSync();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // 2) Called when the player’s hand is empty
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        // Only toggle on *sneak* right-click; otherwise do nothing
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            boolean nowOpen = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, nowOpen), net.minecraft.world.level.block.Block.UPDATE_ALL);

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WoodGasValveBlockEntity valve) {
                valve.setDirtyAndSync();
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Keep side booleans fresh when neighbors change
    @Override
    public BlockState updateShape(BlockState state, Direction changedSide, BlockState changedState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return recalcOneConnection(level, pos, state, changedSide);
    }

    private BlockState recalcOneConnection(LevelAccessor level, BlockPos pos, BlockState state, Direction dir) {
        boolean connected = hasFluidNeighbor(level, pos.relative(dir), dir.getOpposite());
        return setDirFlag(state, dir, connected);
    }

    private BlockState recalcAllConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            boolean connected = hasFluidNeighbor(level, pos.relative(d), d.getOpposite());
            state = setDirFlag(state, d, connected);
        }
        return state;
    }

    private static BlockState setDirFlag(BlockState s, Direction d, boolean value) {
        return switch (d) {
            case NORTH -> s.setValue(NORTH, value);
            case SOUTH -> s.setValue(SOUTH, value);
            case EAST  -> s.setValue(EAST,  value);
            case WEST  -> s.setValue(WEST,  value);
            case UP    -> s.setValue(UP,    value);
            case DOWN  -> s.setValue(DOWN,  value);
        };
    }

    /**
     * Return true if the neighbor at neighborPos exposes a fluid handler on side 'onSide'.
     * TODO: Replace the body with your project’s capability helper (same one you use for pipes/flares).
     */
    private static boolean hasFluidNeighbor(LevelAccessor level, BlockPos neighborPos, Direction onSide) {
        if (!(level instanceof Level lvl)) return false;
        BlockState ns = lvl.getBlockState(neighborPos);
        BlockEntity nbe = lvl.getBlockEntity(neighborPos);

        // --- Example patterns (pick what matches your codebase) ---
        // 1) If you tag your own pipe/valve/tank blocks, you could fast-path here:
        // if (ns.getBlock() instanceof WoodGasPipeBlock || ns.getBlock() instanceof WoodGasValveBlock) return true;

        // 2) Generic capability probe (NeoForge block capability):
        try {
            var handler = lvl.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    neighborPos, ns, nbe, onSide);
            return handler != null;
        } catch (Throwable ignored) {
            // Fallback for older wrappers or during dev reloads
        }
        return false;
    }

    // ───────────────── Block Entity glue ─────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodGasValveBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.WOODGAS_VALVE_BE.get(),
                WoodGasValveBlockEntity::serverTick);
    }


    // Optional: nicer occlusion if your model is small
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return false;
    }

    @Override
    public boolean hasDynamicShape() {
        return true;
    }
}
