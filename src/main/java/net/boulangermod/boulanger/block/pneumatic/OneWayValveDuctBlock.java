package net.boulangermod.boulanger.block.pneumatic;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.pneumatic.blockentity.OneWayValveDuctBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class OneWayValveDuctBlock extends ValveDuctBlock {

    public static final MapCodec<OneWayValveDuctBlock> CODEC = simpleCodec(OneWayValveDuctBlock::new);

    /**
     * Direction the check valve allows flow "toward".
     * Items/air may exit this block ONLY in FLOW_DIR.
     *
     * NOTE: This is intentionally the same underlying property as ValveDuctBlock.FACING
     * so we do NOT introduce additional blockstate dimensions.
     */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FLOW = ValveDuctBlock.FACING;

    public OneWayValveDuctBlock(Properties properties) {
        super(properties);

        // Default; overwritten on placement.
        this.registerDefaultState(this.defaultBlockState()
                .setValue(OPEN, true)
                .setValue(FACING, Direction.EAST));
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OneWayValveDuctBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Mirrors PneumaticDuctBlock behavior (axis is perpendicular to the player)
        BlockState s = super.getStateForPlacement(ctx);

        // Pick a direction along that perpendicular axis for the valve model orientation
        Direction axisDir = ctx.getHorizontalDirection().getClockWise();

        return s.setValue(OPEN, true)
                .setValue(FACING, axisDir);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Shift-click flips flow direction (i.e., FACING)
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;

            BlockState out = state.setValue(FACING, state.getValue(FACING).getOpposite());
            level.setBlock(pos, out, Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.35f, 1.0f);

            notifyTopology(level, pos);
            return InteractionResult.CONSUME;
        }

        // Normal click toggles OPEN (ValveDuctBlock behavior)
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
