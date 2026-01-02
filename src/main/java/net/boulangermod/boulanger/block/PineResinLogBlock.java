package net.boulangermod.boulanger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;


public class PineResinLogBlock extends RotatedPillarBlock {
    public static final BooleanProperty HAS_RESIN = BooleanProperty.create("has_resin");

    public static final int MAX_CHARGES = 5;
    public static final IntegerProperty RESIN_REMAINING =
            IntegerProperty.create("resin_remaining", 0, MAX_CHARGES);


    public static final int REFILL_DELAY_TICKS = 20 * 120;

    public PineResinLogBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(HAS_RESIN, false)
                .setValue(RESIN_REMAINING, 0) // ← was MAX_CHARGES
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(HAS_RESIN, RESIN_REMAINING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(AXIS, ctx.getClickedFace().getAxis())
                .setValue(HAS_RESIN, false)
                .setValue(RESIN_REMAINING, 0);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        int charges = state.getValue(RESIN_REMAINING);
        if (charges > 0 && !state.getValue(HAS_RESIN)) {
            world.setBlock(pos, state.setValue(HAS_RESIN, true), 2);
        }
    }

    @Override
    public @Nullable BlockState getToolModifiedState(
            BlockState state,
            net.minecraft.world.item.context.UseOnContext context,
            net.neoforged.neoforge.common.ItemAbility ability,
            boolean simulate
    ) {
        if (ability == net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP) {
            BlockState out = null;

            if (state.is(ModBlocks.PINE_LOG.get())) {
                out = ModBlocks.STRIPPED_PINE_LOG.get().defaultBlockState();
            }

            if (out == null && state.is(ModBlocks.PINE_WOOD.get())) {
                out = ModBlocks.STRIPPED_PINE_WOOD.get().defaultBlockState();
            }

            if (out != null) {
                return out.setValue(AXIS, state.getValue(AXIS));
            }
        }
        return super.getToolModifiedState(state, context, ability, simulate);
    }

}
