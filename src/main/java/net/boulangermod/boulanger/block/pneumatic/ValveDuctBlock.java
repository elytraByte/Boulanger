//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.block.pneumatic;
//
//import com.mojang.serialization.MapCodec;
//import net.boulangermod.boulanger.pneumatic.blockentity.ValveDuctBlockEntity;
//import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.InteractionResult;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.context.BlockPlaceContext;
//import net.minecraft.world.level.BlockGetter;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.BaseEntityBlock;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.Mirror;
//import net.minecraft.world.level.block.Rotation;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.block.state.StateDefinition;
//import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//import net.minecraft.world.level.block.state.properties.BooleanProperty;
//import net.minecraft.world.level.block.state.properties.DirectionProperty;
//import net.minecraft.world.phys.BlockHitResult;
//import net.minecraft.world.phys.shapes.CollisionContext;
//import net.minecraft.world.phys.shapes.Shapes;
//import net.minecraft.world.phys.shapes.VoxelShape;
//import org.jetbrains.annotations.Nullable;
//
//public class ValveDuctBlock extends PneumaticDuctBlock {
//
//    public static final MapCodec<ValveDuctBlock> CODEC = simpleCodec(ValveDuctBlock::new);
//
//    /** Controls model swap: valve vs valve_closed */
//    public static final BooleanProperty OPEN = BooleanProperty.create("open");
//    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
//
//    // Voxel shape is a straight pipe with a taller central housing.
//    // Excludes the handwheel/turn wheel modeled on top.
//    private static final VoxelShape PIPE_Z = box(6, 6, 0, 10, 10, 16);
//    private static final VoxelShape HOUSING_Z = box(5, 4, 4, 11, 13, 12);
//    private static final VoxelShape SHAPE_Z = Shapes.or(PIPE_Z, HOUSING_Z);
//
//    private static final VoxelShape PIPE_X = box(0, 6, 6, 16, 10, 10);
//    private static final VoxelShape HOUSING_X = box(4, 4, 5, 12, 13, 11);
//    private static final VoxelShape SHAPE_X = Shapes.or(PIPE_X, HOUSING_X);
//
//    public ValveDuctBlock(Properties properties) {
//        super(properties);
//        // Preserve the super defaults (axis + duct sides), then add valve-specific props.
//        this.registerDefaultState(this.defaultBlockState()
//                .setValue(OPEN, true)
//                .setValue(FACING, Direction.NORTH));
//    }
//
//    @Override
//    protected MapCodec<? extends BaseEntityBlock> codec() {
//        return CODEC;
//    }
//
//    @Override
//    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
//        super.createBlockStateDefinition(b);
//        b.add(OPEN, FACING);
//    }
//
//    @Override
//    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
//        // Mirror the duct placement behavior (axis perpendicular to player), and keep FACING in sync.
//        BlockState state = super.getStateForPlacement(ctx);
//        Direction axisDir = ctx.getHorizontalDirection().getClockWise();
//        return state
//                .setValue(OPEN, true)
//                .setValue(FACING, axisDir);
//    }
//
//    @Override
//    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
//        return new ValveDuctBlockEntity(pos, state);
//    }
//
//    /**
//     * - Sneak + empty hand: keep PneumaticDuct behavior (remove flange / side toggles)
//     * - Normal empty-hand right click: toggle OPEN (valve ↔ valve_closed) + rebuild networks
//     */
//    @Override
//    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
//        if (player.isShiftKeyDown()) {
//            return super.useWithoutItem(state, level, pos, player, hit);
//        }
//
//        if (level.isClientSide) return InteractionResult.SUCCESS;
//
//        BlockState out = state.cycle(OPEN);
//        level.setBlock(pos, out, Block.UPDATE_CLIENTS);
//
//        level.playSound(
//                null,
//                pos,
//                SoundEvents.LEVER_CLICK,
//                SoundSource.BLOCKS,
//                0.35f,
//                out.getValue(OPEN) ? 0.8f : 0.6f
//        );
//
//        notifyTopology(level, pos);
//        return InteractionResult.CONSUME;
//    }
//
//    protected static void notifyTopology(Level level, BlockPos pos) {
//        if (level.isClientSide) return;
//
//        BlockEntity be = level.getBlockEntity(pos);
//        if (be instanceof ValveDuctBlockEntity valveBe) {
//            valveBe.notifyAirTopologyChanged();
//        } else {
//            AirNetworkManager.get(level).enqueueTopologyChange(pos);
//        }
//    }
//
//    @Override
//    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
//        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
//    }
//
//    @Override
//    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
//        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
//    }
//
//    @Override
//    public BlockState rotate(BlockState state, Rotation rot) {
//        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
//    }
//
//    @Override
//    public BlockState mirror(BlockState state, Mirror mirror) {
//        return state.rotate(mirror.getRotation(state.getValue(FACING)));
//    }
//}
