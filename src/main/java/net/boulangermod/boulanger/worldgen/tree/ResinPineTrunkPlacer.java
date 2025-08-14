package net.boulangermod.boulanger.worldgen.tree;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.PineResinLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.List;
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
                                                            BiConsumer<BlockPos, BlockState> set, RandomSource rand,
                                                            int freeTreeHeight, BlockPos pos, TreeConfiguration cfg) {

        // Place vanilla straight trunk first
        var foliage = super.placeTrunk(level, set, rand, freeTreeHeight, pos, cfg);

        // Collect trunk positions
        var trunk = new ArrayList<BlockPos>(freeTreeHeight);
        for (int dy = 0; dy < freeTreeHeight; dy++) trunk.add(pos.above(dy));

        // 1) HARD RESET: make every trunk log "normal bark" (0 charges, not ready)
        BlockState normal = ModBlocks.PINE_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
                .setValue(PineResinLogBlock.RESIN_REMAINING, 0)
                .setValue(PineResinLogBlock.HAS_RESIN, false);
        for (BlockPos p : trunk) set.accept(p, normal);

        // (optional) avoid very bottom/top
        List<BlockPos> candidates = trunk.size() > 2 ? trunk.subList(1, trunk.size() - 1) : trunk;

        // 2) SEED 1–2 taps
        int taps = 1 + rand.nextInt(2); // 1..2
        for (int i = 0; i < taps && !candidates.isEmpty(); i++) {
            BlockPos tap = candidates.remove(rand.nextInt(candidates.size()));
            BlockState resin = ModBlocks.PINE_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
                    .setValue(PineResinLogBlock.RESIN_REMAINING, PineResinLogBlock.MAX_CHARGES)
                    .setValue(PineResinLogBlock.HAS_RESIN, true);
            set.accept(tap, resin);
        }

        return foliage;
    }
}
