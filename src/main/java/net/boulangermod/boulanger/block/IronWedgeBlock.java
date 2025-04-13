package net.boulangermod.boulanger.block;

import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class IronWedgeBlock extends Block {
    public static final int MAX_DURABILITY = 512;
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, MAX_DURABILITY);

    public IronWedgeBlock(Properties props) {
        super(props);
        // start at whatever the item had
        this.registerDefaultState(this.stateDefinition.any().setValue(DAMAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(DAMAGE);
    }

    // When placed, copy the item’s current damage into the blockstate
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        int dmg = stack.getDamageValue();  // how much durability the item has already used
        return this.defaultBlockState().setValue(DAMAGE, dmg);
    }

    /** Damage the wedge by `amount`, return the new damage. **/
    public static int damageWedge(Level level, BlockPos pos, int amount) {
        BlockState st = level.getBlockState(pos);
        int old = st.getValue(DAMAGE);
        int next = old + amount;
        if (next < MAX_DURABILITY) {
            level.setBlock(pos, st.setValue(DAMAGE, next), 3);
            return next;
        } else {
            // durability exhausted
            level.destroyBlock(pos, false);
            return MAX_DURABILITY;
        }
    }
}
