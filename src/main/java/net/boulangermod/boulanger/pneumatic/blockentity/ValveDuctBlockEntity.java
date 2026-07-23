//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.blockentity;
//
//import net.boulangermod.boulanger.block.pneumatic.ValveDuctBlock;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.level.block.state.BlockState;
//
//public class ValveDuctBlockEntity extends PneumaticDuctBlockEntity {
//
//    public ValveDuctBlockEntity(BlockPos pos, BlockState state) {
//        super(pos, state);
//    }
//
//    @Override
//    public boolean isSideBlocked(Direction dir) {
//        BlockState state = getBlockState();
//
//        // Closed => block everything
//        if (state.getBlock() instanceof ValveDuctBlock && !state.getValue(ValveDuctBlock.OPEN)) {
//            return true;
//        }
//
//        // Open => inline only (FACING and opposite)
//        if (state.getBlock() instanceof ValveDuctBlock) {
//            Direction facing = state.getValue(ValveDuctBlock.FACING);
//            return dir != facing && dir != facing.getOpposite();
//        }
//
//        return super.isSideBlocked(dir);
//    }
//}
