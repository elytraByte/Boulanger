package net.boulangermod.boulanger.block.crop;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.content.WheatVariety;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.Nullable;

public class BoulangerWheatCrop extends CropBlock {

    public static final EnumProperty<WheatVariety> VARIETY =
            EnumProperty.create("variety", WheatVariety.class);

    public BoulangerWheatCrop(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(this.getAgeProperty(), 0)
                .setValue(VARIETY, WheatVariety.HARD_RED_WINTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIETY);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        if (base == null) return null;

        ItemStack stack = context.getItemInHand();
        WheatVariety variety = stack.get(ModDataComponentTypes.WHEAT_VARIETY.get());
        if (variety == null) variety = WheatVariety.HARD_RED_WINTER;

        return base
                .setValue(this.getAgeProperty(), 0)
                .setValue(VARIETY, variety);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this.getBaseSeedId());
        stack.set(ModDataComponentTypes.WHEAT_VARIETY.get(), state.getValue(VARIETY));
        return stack;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getRawBrightness(pos, 0) < 9) return;

        int age = this.getAge(state);
        if (age >= this.getMaxAge()) return;

        float growthSpeed = getGrowthSpeed(state, level, pos);
        boolean shouldGrowByChance = random.nextInt((int) (25.0F / growthSpeed) + 1) == 0;

        if (!CommonHooks.canCropGrow(level, pos, state, shouldGrowByChance)) return;

        BlockState newState = state.setValue(this.getAgeProperty(), age + 1);
        level.setBlock(pos, newState, 2);
        CommonHooks.fireCropGrowPost(level, pos, newState);
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        int age = this.getAge(state);
        int newAge = Math.min(age + this.getBonemealAgeIncrease(level), this.getMaxAge());
        if (newAge == age) return;

        if (!CommonHooks.canCropGrow(level, pos, state, true)) return;

        BlockState newState = state.setValue(this.getAgeProperty(), newAge);
        level.setBlock(pos, newState, 2);
        CommonHooks.fireCropGrowPost(level, pos, newState);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.WHEAT_SEEDS;
    }
}
