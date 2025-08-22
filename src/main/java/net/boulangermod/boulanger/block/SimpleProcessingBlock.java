// SimpleProcessingBlock.java
package net.boulangermod.boulanger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Convenience base for blocks whose block entity construction is simple.
 * Subclasses supply a factory and only need to implement {@link #codec()} and (optionally) shapes.
 */
public abstract class SimpleProcessingBlock extends AbstractProcessingBlock {

    @FunctionalInterface
    public interface BlockEntityFactory extends BiFunction<BlockPos, BlockState, BlockEntity> {
        @Override
        BlockEntity apply(BlockPos pos, BlockState state);
    }

    private final BlockEntityFactory factory;

    protected SimpleProcessingBlock(Properties properties, BlockEntityFactory factory) {
        super(properties);
        this.factory = factory;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }
}
