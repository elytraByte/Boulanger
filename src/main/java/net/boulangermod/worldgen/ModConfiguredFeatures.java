package net.boulangermod.worldgen;


import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.boulangermod.Boulanger;
import net.boulangermod.block.ModBlocks;

public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_WHEAT_KEY = registerKey("wild_wheat_key");

    public static final ResourceKey<ConfiguredFeature<?, ?>> PINE_TREE_KEY = registerKey("pine_tree_key");

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
                                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.WILD_WHEAT.get())), BlockPredicate.matchesBlocks(Blocks.SHORT_GRASS))));


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
                new TwoLayersFeatureSize(0, 3, 5)).dirt(BlockStateProvider.simple(Blocks.TERRACOTTA)).build());
    }



    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}




