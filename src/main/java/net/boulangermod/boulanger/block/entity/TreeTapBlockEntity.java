package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.ModBlocks;
import net.boulangermod.boulanger.block.PineResinLogBlock;
import net.boulangermod.boulanger.block.TreeTapBlock;
import net.boulangermod.boulanger.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class  TreeTapBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TICKS_PER_DRIP = 20 * 90; // drip every 30s
    private int dripTimer = 0;
    private int storedResin = 0;

    public TreeTapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TREE_TAP_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TreeTapBlockEntity tap) {
        Direction dir = state.getValue(TreeTapBlock.FACING).getOpposite();
        BlockPos logPos = pos.relative(dir);
        BlockState logSt = level.getBlockState(logPos);

        if (logSt.getBlock() instanceof PineResinLogBlock) {
            int charges = logSt.getValue(PineResinLogBlock.RESIN_REMAINING);
            boolean hasResin = logSt.getValue(PineResinLogBlock.HAS_RESIN);


            if (charges > 0 && hasResin) {
                BlockState newLogSt = logSt
                        .setValue(PineResinLogBlock.HAS_RESIN, false)
                        .setValue(PineResinLogBlock.RESIN_REMAINING, charges - 1);
                level.setBlock(logPos, newLogSt, 2);

                int gained = 1 + level.random.nextInt(3);
                tap.storedResin += gained;
                tap.setChanged();

                LOGGER.info("TreeTap @ {} tapped log @ {}; gained={} resin (now stored={}), charges left={}",
                        pos, logPos, gained, tap.storedResin, charges - 1
                );

                if (charges - 1 > 0) {
                    LOGGER.info("TreeTap @ {} scheduling log @ {} refill in {} ticks",
                            pos, logPos, PineResinLogBlock.REFILL_DELAY_TICKS
                    );
                    level.scheduleTick(
                            logPos,
                            ModBlocks.PINE_LOG.get(),
                            PineResinLogBlock.REFILL_DELAY_TICKS
                    );
                } else {
                    LOGGER.info("TreeTap @ {} log @ {} is now exhausted (no more charges)", pos, logPos);
                }
            }
        }

        tap.dripTimer = 0;
    }

    public boolean tryCollect(Player player) {
        if (storedResin <= 0) return false;

        ItemStack drop = new ItemStack(ModItems.PINE_RESIN.get(), storedResin);
        if (!player.getInventory().add(drop)) {
            player.spawnAtLocation(drop);
        }

        storedResin = 0;
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("DripTimer", this.dripTimer);
        tag.putInt("StoredResin", this.storedResin);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.dripTimer    = tag.getInt("DripTimer");
        this.storedResin = tag.getInt("StoredResin");
    }

}
