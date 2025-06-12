package net.boulangermod.boulanger.fluid;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.Rarity;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.SoundActions;


public class ModFluids {
    // 1) register your FluidType
    public static final DeferredRegister<FluidType> FLUID_TYPES =
                    DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Boulanger.MODID);

    // 2) register your Fluid instances (still & flowing)
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Boulanger.MODID);

    // 3) block form
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.createBlocks(Boulanger.MODID);

    // 4) bucket item
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            DeferredRegister.createItems(Boulanger.MODID);


    public static final DeferredHolder<FluidType, FluidType> WOOD_GAS_TYPE =
            FLUID_TYPES.register("wood_gas",
                    () -> new FluidType(
                            FluidType.Properties.create()
                                    .density(1000)
                                    .viscosity(250)
                                    .rarity(Rarity.UNCOMMON)
                                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    )
            );

    public static final DeferredHolder<Fluid, WoodGasFluid.Source>
            WOOD_GAS_STILL = FLUIDS.register("wood_gas_still", WoodGasFluid.Source::new);

    public static final DeferredHolder<Fluid, WoodGasFluid.Flowing>
            WOOD_GAS_FLOWING = FLUIDS.register("wood_gas_flowing", WoodGasFluid.Flowing::new);


    public static final DeferredHolder<Block, LiquidBlock> WOOD_GAS_BLOCK =
            BLOCKS.register("wood_gas",
                    () -> new LiquidBlock(
                            WOOD_GAS_STILL.get(),
                            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    )
            );

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
