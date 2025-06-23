package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;

import java.util.function.Predicate;

/**
 * Very small helper implementation using Minecraft's {@link BlockPattern}
 * to validate and place structures.
 */
public class SimpleMultiblock extends AbstractMultiblock {
    private final String name;
    private final BlockPattern pattern;
    private final BlockPos size;

    public SimpleMultiblock(String name, BlockPattern pattern, BlockPos size) {
        this.name = name;
        this.pattern = pattern;
        this.size = size;
    }

    @Override
    public String getUniqueName() {
        return name;
    }

    @Override
    public BlockPos getSize() {
        return size;
    }

    @Override
    public boolean isBlockTrigger(BlockState state) {
        return true;
    }

    @Override
    public boolean create(Level level, BlockPos pos, Direction facing, Player player) {
        BlockPattern.BlockPatternMatch helper = pattern.find(level, pos);
        if (helper == null) return false;
        // the pattern simply validates, actual placement is up to the caller
        return true;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Small builder around {@link BlockPatternBuilder}.
     */
    public static class Builder {
        private final String name;
        private final BlockPatternBuilder delegate = BlockPatternBuilder.start();
        private int width;
        private int height;
        private int depth;

        private Builder(String name) {
            this.name = name;
        }

        public Builder where(char symbol, Predicate<BlockInWorld> predicate) {
            delegate.where(symbol, predicate);
            return this;
        }

        public Builder aisle(String... rows) {
            delegate.aisle(rows);
            width = Math.max(width, rows[0].length());
            height++;
            depth = Math.max(depth, rows.length);
            return this;
        }

        public SimpleMultiblock build() {
            BlockPos size = new BlockPos(width, height, depth);
            return new SimpleMultiblock(name, delegate.build(), size);
        }
    }
}