package net.boulangermod.boulanger.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PineResinLogBlock extends RotatedPillarBlock {
    public static final BooleanProperty HAS_RESIN = BooleanProperty.create("has_resin");
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int MAX_CHARGES = 5;
    public static final IntegerProperty RESIN_REMAINING =
            IntegerProperty.create("resin_remaining", 0, MAX_CHARGES);


    public static final int REFILL_DELAY_TICKS = 20 * 120;

    public PineResinLogBlock(Properties props) {
        super(props); // no randomTicks()
        this.registerDefaultState(this.defaultBlockState()
                .setValue(AXIS, Axis.Y)
                .setValue(HAS_RESIN, false)
                .setValue(RESIN_REMAINING, 0) // ← was MAX_CHARGES
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block,BlockState> b) {
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
            LOGGER.info("PineResinLog @ {} refilled (charges remaining={})", pos, charges);
        }
    }
}

