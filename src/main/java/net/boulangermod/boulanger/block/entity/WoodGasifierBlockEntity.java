package net.boulangermod.boulanger.block.entity;

import net.boulangermod.boulanger.block.WoodGasifierBlock;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.multiblock.IMultiblock;
import net.boulangermod.boulanger.multiblock.MultiblockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

import static net.boulangermod.boulanger.block.WoodGasifierBlock.FACING;

public class WoodGasifierBlockEntity extends AbstractProcessingBlockEntity {
    // slots for cooking
    private static final int SPLIT_SLOT = 0;
    private static final int LOG_SLOT   = 1;

    // timing & output
    private static final int BURN_TIME_PER_LOG = 300;
    private static final int WOOD_GAS_PER_COOK = 1000;

    private boolean formed = false;
    private int burnTime = 0;
    private final FluidTank woodGasTank = new FluidTank(8_000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public static int getBurnTimePerLog() {
        return BURN_TIME_PER_LOG;
    }

    // hook to your registered multiblock
    private static final IMultiblock MULTIBLOCK =
            MultiblockRegistry.get("wood_gasifier");

    public WoodGasifierBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GASIFIER_BE.get(), pos, st, 2);
    }

    public boolean isFormed() {
        return formed;
    }

    public BlockPos getMultiblockOrigin() {
        // local offset: (-1, 0, 0) moves one block in negative X from master
        BlockPos localOrigin = new BlockPos(-1, 0, 0);
        Direction facing = getBlockState().getValue(FACING);
        BlockPos rotated = rotateOffset(localOrigin, facing);
        return worldPosition.offset(rotated);
    }

    /**
     * Rotates a local offset vector around the Y axis based on the block's facing.
     * @param local  the offset in the block's local coordinate space
     * @param face   the facing of the master block
     * @return       the rotated offset
     */
    private BlockPos rotateOffset(BlockPos local, Direction face) {
        return switch (face) {
            case EAST  -> new BlockPos(-local.getZ(), local.getY(),  local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case WEST  -> new BlockPos( local.getZ(), local.getY(), -local.getX());
            default    -> local;  // NORTH
        };
    }


    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.WOOD_GASIFIER_BE.get();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }


    /** Server‐side ticker: only run when “formed.” */
    public static void tick(Level level, BlockPos pos, BlockState state, WoodGasifierBlockEntity be) {
        if (level.isClientSide || !be.formed) return;
        be.performCookingTick();
    }

    private void performCookingTick() {
        if (burnTime <= 0 && canCook()) {
            doCook();
            burnTime = BURN_TIME_PER_LOG;
        }
        if (burnTime > 0) burnTime--;
    }

    private boolean canCook() {
        IItemHandler items = getItemHandler();
        ItemStack split = items.getStackInSlot(SPLIT_SLOT);
        ItemStack log   = items.getStackInSlot(LOG_SLOT);
        boolean validLog = log.is(Items.OAK_LOG)
                || log.is(Items.BIRCH_LOG)
                || log.is(Items.SPRUCE_LOG);
        return validLog
                && split.getItem() == ModItems.SPLIT_PINE_LOGS.get()
                && split.getCount() > 0;
    }

    private void doCook() {
        // pull your unified handler directly
        IItemHandler items = getItemHandler();

        items.extractItem(SPLIT_SLOT, 1, false);
        items.extractItem(LOG_SLOT,   1, false);

        woodGasTank.fill(
                new FluidStack(ModFluids.WOOD_GAS_STILL.get(), WOOD_GAS_PER_COOK),
                IFluidHandler.FluidAction.EXECUTE
        );

        setChanged();
    }


    /** Called from your block’s `use(...)` override */
    public InteractionResult tryToggleForm(Player player, InteractionHand hand, Direction face) {
        if (level == null || level.isClientSide) return InteractionResult.SUCCESS;

        if (!formed) {
            // only trigger if this block is the designated “trigger”
            if (!MULTIBLOCK.isBlockTrigger(level.getBlockState(worldPosition)))
                return InteractionResult.PASS;

            // attempt to assemble
            if (MULTIBLOCK.create(level, worldPosition, face, player)) {
                setFormed(true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        } else {
            // dismantle
            MULTIBLOCK.disassemble(level, worldPosition);
            setFormed(false);
            return InteractionResult.CONSUME;
        }
    }

    public void setFormed(boolean formed) {
        this.formed = formed;

        if (level == null) return;
        BlockState current = level.getBlockState(worldPosition);
        Block block = current.getBlock();
        if (!(block instanceof WoodGasifierBlock)) {
            // not our block (yet), bail out
            return;
        }

        // make sure the property exists before trying to set it
        BlockState updated = current;
        if (current.hasProperty(WoodGasifierBlock.FORMED)) {
            updated = updated.setValue(WoodGasifierBlock.FORMED, formed);
        }
        if (current.hasProperty(WoodGasifierBlock.HIDDEN)) {
            updated = updated.setValue(WoodGasifierBlock.HIDDEN, !formed);
        }

        // only write it if something changed
        if (updated != current) {
            level.setBlock(worldPosition, updated, 3);
        }
    }


    // NBT save/load just for the “formed” flag + burnTime + tank
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("formed", formed);
        tag.putInt("burnTime", burnTime);
        tag.put("tank", woodGasTank.writeToNBT(regs, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("formed"))    setFormed(tag.getBoolean("formed"));
        if (tag.contains("burnTime"))   burnTime = tag.getInt("burnTime");
        if (tag.contains("tank"))       woodGasTank.readFromNBT(regs, tag.getCompound("tank"));
    }

    @Override
    public Component getDisplayName() {
        return null;
    }
}
