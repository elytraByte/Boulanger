package org.l3e.boulanger.item;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.l3e.boulanger.block.entity.WoodGasifierBlockEntity;

public class WrenchToolItem extends Item {


    public WrenchToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();

        if(!level.isClientSide()) {
            if(level.getBlockState(context.getClickedPos()).is(Blocks.FURNACE)) {

                //level.setBlockEntity(new WoodGasifierBlockEntity(context.getClickedPos(), context.getLevel().getBlockState(context.getClickedPos()).getValue(Direction.byName("facing"))));


            }
        }

        return super.useOn(context);
    }
}
