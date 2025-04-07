package net.boulangermod.boulanger.worldgen;


import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.block.ModBlocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_WHEAT_KEY = registerKey("wild_wheat_key");

    public static final ResourceKey<ConfiguredFeature<?, ?>> PINE_TREE_KEY = registerKey("pine_tree_key");

    public static final ResourceKey<ConfiguredFeature<?, ?>> KAOLINITE_PATCH_KEY = registerKey("kaolinite_patch_key");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {

         new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.WILD_WHEAT.get()));

        register(
                context,
                WILD_WHEAT_KEY,
                Feature.RANDOM_PATCH,
                new RandomPatchConfiguration(
                        96,
                        64,
                        1,
                        PlacementUtils.filtered(
                                Feature.SIMPLE_BLOCK,
                                new SimpleBlockConfiguration(BlockStateProvider
                                        .simple(ModBlocks.WILD_WHEAT.get())),
                                BlockPredicate.matchesBlocks(Blocks.SHORT_GRASS))));


//        register(context, PINE_TREE_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
//                BlockStateProvider.simple(ModBlocks.PINE_LOG.get()),
//                new StraightTrunkPlacer(12, 15, 17),
//                BlockStateProvider.simple(ModBlocks.PINE_LEAVES.get()),
//                new PineFoliagePlacer(ConstantInt.of(3), ConstantInt.of(4), ConstantInt.of(10)),
//                new TwoLayersFeatureSize(1, 0, 2)).build());


        register(context, PINE_TREE_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.PINE_LOG.get()),
                new StraightTrunkPlacer(8, 5, 5),
                BlockStateProvider.simple(ModBlocks.PINE_LEAVES.get()),
                new SpruceFoliagePlacer(ConstantInt.of(3), ConstantInt.of(5), ConstantInt.of(4)),
                new TwoLayersFeatureSize(0, 3, 5))
                .dirt(BlockStateProvider.simple(Blocks.TERRACOTTA)).build());

        BlockPredicate replaceSandOrDirt = BlockPredicate.matchesBlocks(Blocks.SAND, Blocks.DIRT);

        // Register the configured feature
        context.register(
                KAOLINITE_PATCH_KEY,
                new ConfiguredFeature<>(
                        Feature.DISK,
                        new DiskConfiguration(
                                RuleBasedBlockStateProvider.simple(ModBlocks.KAOLINITE_CLAY.get()), // This is the block to place
                                BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.SAND),     // Replace dirt and sand
                                UniformInt.of(1, 4),  // Radius (random between 2–5)
                                2                     // Vertical thickness (halfHeight)
                        )
                )
        );

    }



    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}




