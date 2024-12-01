package org.l3e.boulanger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.l3e.boulanger.block.entity.MultiBlockMasterBlockEntity;
import org.l3e.boulanger.block.entity.WoodGasifierBlockEntity;
import org.l3e.boulanger.util.ModTags;

public class MultiBlockMasterBlock extends Block {
    public MultiBlockMasterBlock(Properties properties) {
        super(properties);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (isStructureValid(level, pos)) { // Check structure validity
                transformStructure(level, pos); // Transform structure
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS; // Return PASS if not valid
    }

    // Method to check if the structure is valid
    private boolean isStructureValid(Level level, BlockPos masterPos) {
        for (int x = 0; x <= 1; x++) { // Width
            for (int y = 0; y <= 2; y++) { // Height
                for (int z = 0; z <= 0; z++) { // Depth
                    BlockPos checkPos = masterPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (!state.is(ModTags.Blocks.MB_MASTER) && !state.is(ModTags.Blocks.MB_SLAVE)) {
                        return false; // Invalid block
                    }
                }
            }
        }
        return true; // Structure is valid
    }

    // Method to transform the structure
    private void transformStructure(Level level, BlockPos masterPos) {
        for (int x = 0; x <= 1; x++) { // Width
            for (int y = 0; y <= 2; y++) { // Height
                for (int z = 0; z <= 0; z++) { // Depth
                    BlockPos checkPos = masterPos.offset(x, y, z);

                    if (x == 0 && y == 0 && z == 0) {
                        // Replace master block with Wood Gasifier and add BlockEntity
                        level.setBlock(checkPos, ModBlocks.WOOD_GASIFIER.get().defaultBlockState(), 3);
                        BlockEntity gasifierEntity = level.getBlockEntity(checkPos);
                        if (gasifierEntity instanceof WoodGasifierBlockEntity) {
                            gasifierEntity.setChanged(); // Mark BlockEntity as updated
                        }
                    } else {
                        // Replace slave blocks with structure blocks (optional)
                        level.setBlock(checkPos, ModBlocks.WOOD_GASIFIER.get().defaultBlockState(), 3);
                    }
                }
            }
        }

        // Play sound to indicate transformation success
        level.playSound(null, masterPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

        // Spawn transformation particles
        for (int i = 0; i < 20; i++) {
            level.addParticle(ParticleTypes.CLOUD,
                    masterPos.getX() + level.random.nextDouble(),
                    masterPos.getY() + level.random.nextDouble(),
                    masterPos.getZ() + level.random.nextDouble(),
                    0, 0.1, 0);
        }
    }

    public void onNeighborChange(BlockState state, Level level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MultiBlockMasterBlockEntity master) {
            master.checkMultiblock(); // Recheck the multiblock structure
        }

    }
}