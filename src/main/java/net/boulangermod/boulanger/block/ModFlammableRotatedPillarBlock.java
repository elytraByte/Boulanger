package net.boulangermod.boulanger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import javax.annotation.Nullable;

public class ModFlammableRotatedPillarBlock extends RotatedPillarBlock {
    public ModFlammableRotatedPillarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state,
                                                     UseOnContext context,
                                                     ItemAbility ability,
                                                     boolean simulate) {
        // Only react to the axe-strip ability. Scrape / wax-off fall through to super.
        if (ability == ItemAbilities.AXE_STRIP) {
            if (state.is(ModBlocks.PINE_LOG.get())) {
                return ModBlocks.STRIPPED_PINE_LOG.get()
                        .defaultBlockState()
                        .setValue(AXIS, state.getValue(AXIS)); // preserve axis
            }
            if (state.is(ModBlocks.PINE_WOOD.get())) {
                return ModBlocks.STRIPPED_PINE_WOOD.get()
                        .defaultBlockState()
                        .setValue(AXIS, state.getValue(AXIS));
            }
        }
        return super.getToolModifiedState(state, context, ability, simulate);
    }

}
