package net.boulangermod.boulanger.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class DecorativePorcelainTileBlock extends Block {
    public static final EnumProperty<PorcelainTileVariant> VARIANT =
            EnumProperty.create("variant", PorcelainTileVariant.class);

    public DecorativePorcelainTileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, PorcelainTileVariant.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }
}

