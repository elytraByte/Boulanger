package net.boulangermod.boulanger.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects render related data for a formed multiblock.
 */
public class MultiblockRenderInfo {
    public final BlockPos masterPos;
    public final List<BlockPos> slavePositions = new ArrayList<>();
    public final AABB bounds;

    public MultiblockRenderInfo(BlockPos masterPos, Iterable<BlockPos> slaves) {
        this.masterPos = masterPos;
        for (BlockPos p : slaves) {
            slavePositions.add(p);
        }
        this.bounds = calculateBounds();
    }

    private AABB calculateBounds() {
        double minX = masterPos.getX();
        double minY = masterPos.getY();
        double minZ = masterPos.getZ();
        double maxX = minX + 1;
        double maxY = minY + 1;
        double maxZ = minZ + 1;
        for (BlockPos p : slavePositions) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX() + 1);
            maxY = Math.max(maxY, p.getY() + 1);
            maxZ = Math.max(maxZ, p.getZ() + 1);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
