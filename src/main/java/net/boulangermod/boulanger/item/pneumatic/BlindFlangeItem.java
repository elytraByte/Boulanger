//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.item.pneumatic;
//
//import net.boulangermod.boulanger.block.pneumatic.DuctSide;
//import net.boulangermod.boulanger.block.pneumatic.PneumaticDuctBlock;
//import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.InteractionResult;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.context.UseOnContext;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.block.state.properties.EnumProperty;
//
//public class BlindFlangeItem extends Item {
//
//    public BlindFlangeItem(Properties props) {
//        super(props);
//    }
//
//    @Override
//    public InteractionResult useOn(UseOnContext ctx) {
//        Level level = ctx.getLevel();
//        BlockPos pos = ctx.getClickedPos();
//
//        BlockState state = level.getBlockState(pos);
//        if (!(state.getBlock() instanceof PneumaticDuctBlock)) {
//            return InteractionResult.PASS;
//        }
//
//        Direction face = ctx.getClickedFace();
//        EnumProperty<DuctSide> prop = PneumaticDuctBlock.propFor(face);
//        DuctSide cur = state.getValue(prop);
//
//        // Binary behavior:
//        // - This item ONLY places a flange (caps a face).
//        // - Removing a flange is handled by the duct block's useWithoutItem().
//        if (cur != DuctSide.OPEN && cur != DuctSide.CONNECTED) {
//            return InteractionResult.PASS;
//        }
//
//        if (level.isClientSide) {
//            return InteractionResult.SUCCESS;
//        }
//
//        level.setBlock(pos, state.setValue(prop, DuctSide.FLANGED), Block.UPDATE_ALL);
//
//        // consume 1 flange in survival
//        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
//            ctx.getItemInHand().shrink(1);
//        }
//
//        AirNetworkManager.get(level).enqueueTopologyChange(pos);
//        return InteractionResult.CONSUME;
//    }
//}
