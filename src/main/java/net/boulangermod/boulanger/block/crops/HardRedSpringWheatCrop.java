package net.boulangermod.boulanger.block.crops;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.item.WheatVariety;
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
import net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension;
import org.jetbrains.annotations.Nullable;

public class HardRedSpringWheatCrop extends CropBlock {

    public static final int MAX_AGE = 7;
    // Use the AGE property from CropBlock (do not redefine it)
    public static final EnumProperty<WheatVariety> VARIETY =
            EnumProperty.create("variety", WheatVariety.class);

    public HardRedSpringWheatCrop(Properties props) {
        super(props);
        // Set the default state with age 0 and the default variety.
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(this.getAgeProperty(), 0)
                .setValue(VARIETY, WheatVariety.HARD_RED_WINTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Let CropBlock register its age property, then add the variety property.
        super.createBlockStateDefinition(builder);
        builder.add(VARIETY);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Read the variety from the seed ItemStack.
        ItemStack stack = context.getItemInHand();
        WheatVariety variety = ((IDataComponentHolderExtension) stack)
                .get(ModDataComponentTypes.WHEAT_VARIETY::get);
        if (variety == null) variety = WheatVariety.HARD_RED_WINTER;
        // Place the crop at age 0 with the seed’s variety.
        return this.defaultBlockState()
                .setValue(this.getAgeProperty(), 0)
                .setValue(VARIETY, variety);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this.getBaseSeedId());
        // Optionally, read the variety from the item’s data (if it was set)
        WheatVariety variety = ((IDataComponentHolderExtension) stack)
                .get(ModDataComponentTypes.WHEAT_VARIETY::get);
        return stack;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    /**
     * Override natural growth so that only the age property is updated,
     * preserving the crop’s variety.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isAreaLoaded(pos, 1)) {
            if (level.getRawBrightness(pos, 0) >= 9) {
                int age = this.getAge(state);
                if (age < this.getMaxAge()) {
                    float growthSpeed = getGrowthSpeed(state, level, pos);
                    if (random.nextInt((int)(25.0F / growthSpeed) + 1) == 0) {
                        // Update only the age property and leave VARIETY unchanged.
                        BlockState newState = state.setValue(this.getAgeProperty(), age + 1);
                        level.setBlock(pos, newState, 2);
                    }
                }
            }
        }
    }

    /**
     * Override crop growth via bonemeal so that the variety is preserved.
     */
    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        int age = this.getAge(state);
        int newAge = age + this.getBonemealAgeIncrease(level);
        if (newAge > this.getMaxAge()) {
            newAge = this.getMaxAge();
        }
        // Update only the age property on the existing state (preserving variety)
        level.setBlock(pos, state.setValue(this.getAgeProperty(), newAge), 2);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.WHEAT_SEED;
    }
}
