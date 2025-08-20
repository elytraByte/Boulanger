package net.boulangermod.boulanger.fluid;

import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class WoodGasFluid extends FlowingFluid {
    public static final int BUCKET_VOLUME = FluidType.BUCKET_VOLUME;

    // *** FIX: make the fluid return your registered FluidType ***
    @Override
    public FluidType getFluidType() {
        return ModFluids.WOOD_GAS_TYPE.get();
    }

    // ─── Shared ───────────────────────────────────────────────
    @Override
    public Item getBucket() {
        return ModItems.WOOD_GAS_BUCKET.get();
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
        builder.add(LEVEL);
    }

    @Override
    protected float getExplosionResistance() { return 100f; }

    @Override
    public Vec3 getFlow(BlockGetter reader, BlockPos pos, FluidState state) {
        return super.getFlow(reader, pos, state);
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter reader, BlockPos pos, Fluid otherFluid, Direction dir) {
        return false;
    }

    @Override
    public int getTickDelay(LevelReader world) {
        return 4;
    }

    // ─── Source ───────────────────────────────────────────────
    public static class Source extends WoodGasFluid {
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return BUCKET_VOLUME; }

        @Override public Fluid getFlowing() { return ModFluids.WOOD_GAS_FLOWING.get(); }
        @Override public Fluid getSource()   { return ModFluids.WOOD_GAS_STILL.get(); }

        @Override
        protected BlockState createLegacyBlock(FluidState state) {
            return ModFluids.WOOD_GAS_BLOCK.get()
                    .defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, state.getValue(LEVEL));
        }

        @Override protected boolean canConvertToSource(Level level) { return false; }
        @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {}

        @Override protected int getSlopeFindDistance(LevelReader world) { return 4; }
        @Override protected int getDropOff(LevelReader world) { return 1; }
    }

    // ─── Flowing ──────────────────────────────────────────────
    public static class Flowing extends WoodGasFluid {
        @Override public boolean isSource(FluidState state) { return false; }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }

        @Override public Fluid getFlowing() { return ModFluids.WOOD_GAS_FLOWING.get(); }
        @Override public Fluid getSource()   { return ModFluids.WOOD_GAS_STILL.get(); }

        @Override
        protected BlockState createLegacyBlock(FluidState state) {
            return ModFluids.WOOD_GAS_BLOCK.get()
                    .defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, state.getValue(LEVEL));
        }

        @Override protected boolean canConvertToSource(Level level) { return false; }
        @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {}

        @Override protected int getSlopeFindDistance(LevelReader world) { return 4; }
        @Override protected int getDropOff(LevelReader world) { return 1; }
    }
}
