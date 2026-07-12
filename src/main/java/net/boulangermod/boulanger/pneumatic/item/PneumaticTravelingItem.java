package net.boulangermod.boulanger.pneumatic.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;

import javax.annotation.Nullable;

/**
 * A single traveling stack inside a duct.
 *
 * - cameFrom: the side on the CURRENT duct that the item arrived from (prevents immediate backtracking)
 * - step: where we are in route[]
 * - route: compact encoded directions (byte ordinals 0..5). May be null (will re-route).
 */
public final class PneumaticTravelingItem {
    public ItemStack stack;
    public Direction cameFrom;
    public int step;
    @Nullable public byte[] route;

    public PneumaticTravelingItem(ItemStack stack, Direction cameFrom, int step, @Nullable byte[] route) {
        this.stack = stack;
        this.cameFrom = cameFrom;
        this.step = step;
        this.route = route;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();

        CompoundTag stackTag = new CompoundTag();
        stack.save(registries, stackTag);
        tag.put("Stack", stackTag);

        tag.putByte("Dir", (byte) cameFrom.ordinal());
        tag.putInt("Step", step);

        if (route != null && route.length > 0) {
            tag.putByteArray("Route", route);
        }

        return tag;
    }

    public static PneumaticTravelingItem load(HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag stackTag = tag.contains("Stack", CompoundTag.TAG_COMPOUND)
                ? tag.getCompound("Stack")
                : new CompoundTag();

        ItemStack stack = ItemStack.parseOptional(registries, stackTag);

        int dirOrd = Mth.clamp(tag.getByte("Dir"), 0, 5);
        Direction cameFrom = Direction.from3DDataValue(dirOrd);

        int step = tag.getInt("Step");

        byte[] route = null;
        if (tag.contains("Route")) {
            byte[] arr = tag.getByteArray("Route");
            if (arr.length > 0) route = arr;
        }

        return new PneumaticTravelingItem(stack, cameFrom, step, route);
    }
}
