package net.boulangermod.boulanger.block;

import com.mojang.serialization.MapCodec;
import net.boulangermod.boulanger.block.entity.WoodOvenBlockEntity;
import net.boulangermod.boulanger.screen.WoodOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WoodOvenBlock extends AbstractProcessingBlock implements MenuProvider {
    public static final MapCodec<WoodOvenBlock> CODEC = simpleCodec(WoodOvenBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;


    public WoodOvenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(LIT, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            Direction direction = state.getValue(FACING).getOpposite();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            double offset = 0.52;

            double fx = x + direction.getStepX() * offset;
            double fz = z + direction.getStepZ() * offset;

            level.addParticle(ParticleTypes.FLAME, fx, y, fz, 0.0, 0.1, 0.0);
            level.addParticle(ParticleTypes.SMOKE, fx, y, fz, 0.0, 0.1, 0.0);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodOvenBlockEntity(pos, state);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        BlockEntity entity = inventory.player.level().getBlockEntity(inventory.player.blockPosition());
        if (entity instanceof WoodOvenBlockEntity oven) {
            return new WoodOvenMenu(id, inventory, oven);
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider provider) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, plyr) -> new WoodOvenMenu(id, inventory, (WoodOvenBlockEntity) entity),
                        Component.translatable("woodoven.boulanger")
                ), pos); // 🡐 This part ensures extraData contains the block pos
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("woodoven.boulanger");
    }

}
