//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.blockentity;
//
//import net.boulangermod.boulanger.block.pneumatic.OneWayValveDuctBlock;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.level.block.state.BlockState;
//
///**
// * Inline-only check valve BE.
// *
// * Physical connectivity (only two ports) is handled by ValveDuctBlockEntity (inline-only).
// * Directionality (one-way) should be enforced by your flow solver using OneWayValveDuctBlock.FLOW_DIR.
// */
//public class OneWayValveDuctBlockEntity extends ValveDuctBlockEntity {
//
//    public OneWayValveDuctBlockEntity(BlockPos pos, BlockState state) {
//        super(pos, state);
//    }
//
//    public Direction flowDir() {
//        BlockState s = getBlockState();
//        return s.getValue(OneWayValveDuctBlock.FLOW);
//    }
//}
