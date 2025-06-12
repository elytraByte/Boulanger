package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.EnergyStorageBlockEntity;
import net.boulangermod.boulanger.block.entity.ModBlockEntities;
import net.boulangermod.boulanger.block.entity.WoodGasPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class WoodGasPipe extends BaseEntityBlock implements EntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    public WoodGasPipe(Properties props) {
        super(props.noOcclusion().randomTicks());
        // set default state: no connections
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST,  false)
                .setValue(SOUTH, false)
                .setValue(WEST,  false)
                .setValue(UP,    false)
                .setValue(DOWN,  false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState()
                .setValue(NORTH, connectsTo(world, pos.relative(Direction.NORTH)))
                .setValue(EAST,  connectsTo(world, pos.relative(Direction.EAST)))
                .setValue(SOUTH, connectsTo(world, pos.relative(Direction.SOUTH)))
                .setValue(WEST,  connectsTo(world, pos.relative(Direction.WEST)))
                .setValue(UP,    connectsTo(world, pos.above()))
                .setValue(DOWN,  connectsTo(world, pos.below()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        BooleanProperty prop = switch (facing) {
            case NORTH -> NORTH;
            case EAST  -> EAST;
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
        return state.setValue(prop, connectsTo(world, facingPos));
    }

    private static boolean connectsTo(LevelAccessor world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof WoodGasPipe;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof WoodGasPipeBlockEntity pipe)) return;
        if (pipe.getTank().getFluidAmount() <= 0) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double angle = random.nextDouble() * Math.PI * 2;
        double vx    = Math.cos(angle) * 0.05;
        double vz    = Math.sin(angle) * 0.05;
        double vy    = 0.02 + random.nextDouble() * 0.02;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, vx, vy, vz);
    }

    @Override
    public RenderShape getRenderShape(BlockState st) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return new WoodGasPipeBlockEntity(pos, st);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState st, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type,
                ModBlockEntities.WOOD_GAS_PIPE_BE.get(),
                WoodGasPipeBlockEntity::tickServer
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state,
                                               Level world,
                                               BlockPos pos,
                                               Player player,
                                               net.minecraft.world.phys.BlockHitResult hit) {
        if (!world.isClientSide) {
            var be = world.getBlockEntity(pos);
            if (be instanceof WoodGasPipeBlockEntity pipe) {
                int mb = pipe.getTank().getFluidAmount();
                player.sendSystemMessage(Component.literal("Pipe contains: " + mb + " mB wood-gas"));
                return InteractionResult.SUCCESS;
            }
            if (be instanceof EnergyStorageBlockEntity batt) {
                int rf = batt.getEnergyStorage(null).getEnergyStored();
                player.sendSystemMessage(Component.literal("Stored energy: " + rf + " RF"));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
