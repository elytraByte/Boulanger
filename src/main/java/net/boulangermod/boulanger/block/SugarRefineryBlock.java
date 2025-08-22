package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.SugarRefineryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SugarRefineryBlock extends SimpleProcessingBlock {
    public static final MapCodec<SugarRefineryBlock> CODEC = simpleCodec(SugarRefineryBlock::new);

    public SugarRefineryBlock(Properties props) {
        super(props, SugarRefineryBlockEntity::new);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Outline / selection box (adjust numbers to your model)
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),  // base
            Block.box(3, 2, 3, 13, 10, 13), // frame/body
            Block.box(5,10, 5, 11, 13, 11)  // upper stone/wheel
    );

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) { return true; }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(10) == 0) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.7D;
            double z = pos.getZ() + 0.5D;
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}