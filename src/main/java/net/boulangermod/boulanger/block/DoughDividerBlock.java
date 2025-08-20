package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.DoughDividerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DoughDividerBlock extends AbstractProcessingBlock {
    public static final MapCodec<DoughDividerBlock> CODEC = simpleCodec(DoughDividerBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 8, 16),
            box(2, 8, 2, 14, 12, 14)
    );

    public DoughDividerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) { return Shapes.empty(); }

    @Override public boolean useShapeForLightOcclusion(BlockState s) { return true; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoughDividerBlockEntity(pos, state);
    }
}
