package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.WoodOvenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class WoodOvenBlock extends SimpleProcessingBlock {
    public static final MapCodec<WoodOvenBlock> CODEC = simpleCodec(WoodOvenBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public WoodOvenBlock(Properties properties) {
        super(properties, WoodOvenBlockEntity::new);
        // Default: face north, not lit
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Model front is FACING.getOpposite(). To make the visible front face the player,
        // set FACING to the player's current looking direction.
        Direction playerDir = context.getHorizontalDirection();
        return this.defaultBlockState()
                .setValue(FACING, playerDir)
                .setValue(LIT, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // adds FACING from parent
        builder.add(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Keep structure rotation/mirroring consistent (maps FACING properly in rotations)
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;

        // Model is authored flipped: visible "front" is the opposite of FACING
        Direction front = state.getValue(FACING).getOpposite();

        int outX = front.getStepX();
        int outZ = front.getStepZ();
        double sideX = -outZ;   // rotate (outX,outZ) 90° CCW
        double sideZ =  outX;

        final double OUT_BASE   = 0.50;
        final double OUT_JITTER = 0.08;
        final double HALF_WIDTH = 0.30;
        final double Y_MIN      = 0.06;
        final double Y_MAX      = 0.26;

        int count = 2 + rand.nextInt(3); // 2..4 per tick
        for (int i = 0; i < count; i++) {
            double sideOffset = (rand.nextDouble() * 2.0 - 1.0) * HALF_WIDTH;
            double outOffset  = OUT_BASE + rand.nextDouble() * OUT_JITTER;
            double y          = pos.getY() + Y_MIN + rand.nextDouble() * (Y_MAX - Y_MIN);

            double x = pos.getX() + 0.5 + outX * outOffset + sideX * sideOffset;
            double z = pos.getZ() + 0.5 + outZ * outOffset + sideZ * sideOffset;

            double vx = outX * (0.01 + rand.nextDouble() * 0.015) + sideX * ((rand.nextDouble() - 0.5) * 0.01);
            double vy = 0.02 + rand.nextDouble() * 0.06;
            double vz = outZ * (0.01 + rand.nextDouble() * 0.015) + sideZ * ((rand.nextDouble() - 0.5) * 0.01);

            level.addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz);

            if (rand.nextFloat() < 0.35f) {
                level.addParticle(ParticleTypes.SMOKE, x, y + 0.02, z, vx * 0.6, vy * 0.6, vz * 0.6);
            }
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
