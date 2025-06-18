package net.boulangermod.boulanger.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.PineResinLogBlock;
import net.boulangermod.boulanger.worldgen.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class ResinPineTrunkPlacer extends StraightTrunkPlacer {
    public static final MapCodec<ResinPineTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
            inst -> trunkPlacerParts(inst).apply(inst, ResinPineTrunkPlacer::new)
    );

    public ResinPineTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }


    @Override
    protected TrunkPlacerType<?> type() {
        return ModTrunkPlacers.RESIN_PINE_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level,
                                                            BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random,
                                                            int freeTreeHeight,
                                                            BlockPos pos,
                                                            TreeConfiguration config) {
        // 1) Place the vanilla pine trunk
        List<FoliagePlacer.FoliageAttachment> attachments = super.placeTrunk(level, blockSetter, random, freeTreeHeight, pos, config);

        // 2) Gather the log positions
        List<BlockPos> placedLogs = new ArrayList<>();
        for (int dy = 0; dy < freeTreeHeight; dy++) {
            placedLogs.add(pos.above(dy));
        }

        // 3) Randomly “tap” 1–2 logs by setting has_resin = true
        int taps = 1 + random.nextInt(3);
        for (int i = 0; i < taps && !placedLogs.isEmpty(); i++) {
            BlockPos tapPos = placedLogs.remove(random.nextInt(placedLogs.size()));
            blockSetter.accept(tapPos,
                    ModBlocks.PINE_LOG.get().defaultBlockState()
                            .setValue(PineResinLogBlock.HAS_RESIN, true)
            );
        }

        return attachments;
    }
}
