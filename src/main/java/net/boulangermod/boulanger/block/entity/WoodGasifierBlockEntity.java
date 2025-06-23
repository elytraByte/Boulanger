package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.WoodGasifierBlock;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.multiblock.AbstractMultiblockMachineEntity;
import net.boulangermod.boulanger.multiblock.AbstractMultiblockSlaveEntity;
import net.boulangermod.boulanger.multiblock.WoodGasifierSlaveBlock;
import net.boulangermod.boulanger.screen.WoodGasifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;

public class WoodGasifierBlockEntity extends AbstractMultiblockMachineEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SPLIT_SLOT   = 0;
    private static final int LOG_SLOT     = 1;
    private static final int FILTER_SLOTA = 2;
    private static final int FILTER_SLOTB = 3;

    public static final int BURN_TIME_PER_LOG = 300;
    private static final int WOOD_GAS_PER_COOK = 1000;
    private static final int FLUID_TRANSFER = 1000;

    private boolean formed = false;

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        if (this.formed != formed) {
            this.formed = formed;
            if (level != null) {
                BlockState current = level.getBlockState(worldPosition);
                // hide or show master block
                level.setBlock(worldPosition,
                        current.setValue(WoodGasifierBlock.HIDDEN, formed),
                        3
                );
            }
        }
    }

    private final FluidTank woodGasTank = new FluidTank(8_000, this::onContentsChanged);
    private int burnTime = 0;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int idx) {
            return switch (idx) {
                case 0 -> burnTime;
                case 1 -> woodGasTank.getFluidAmount();
                case 2 -> woodGasTank.getCapacity();
                default -> 0;
            };
        }
        @Override public void set(int idx, int val) {
            if (idx == 0) {
                burnTime = val;
            } else if (idx == 1) {
                FluidStack cur = woodGasTank.getFluid();
                if (cur.isEmpty()) {
                    woodGasTank.setFluid(new FluidStack(ModFluids.WOOD_GAS_STILL.get(), val));
                } else {
                    woodGasTank.setFluid(cur.copyWithAmount(val));
                }
            }
        }
        @Override public int getCount() { return 3; }
    };

    public BlockPos getMultiblockOrigin() {
        return worldPosition;
    }

    public ContainerData getContainerData() {
        return this.data;
    }

    public WoodGasifierBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GASIFIER_BE.get(), pos, st, 4);
    }

    public static void ticker(Level level, BlockPos pos, BlockState state, WoodGasifierBlockEntity be) {
        if (level.isClientSide) return;
        if (!be.isFormed()) return;
        LOGGER.debug("[Gasifier] tick @ {}: burnTime={}, tank={}/{}mB",
                pos, be.burnTime, be.woodGasTank.getFluidAmount(), be.woodGasTank.getCapacity());
        be.tickServer(level, pos);
    }

    private void tickServer(Level level, BlockPos pos) {
        if (burnTime <= 0 && canCook()) {
            doCook();
            burnTime = BURN_TIME_PER_LOG;
        }
        if (burnTime > 0) burnTime--;

        for (Direction dir : Direction.values()) {
            var neigh = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    pos.relative(dir),
                    dir.getOpposite()
            );
            if (neigh == null || woodGasTank.getFluidAmount() == 0) continue;

            int want = Math.min(woodGasTank.getFluidAmount(), FLUID_TRANSFER);
            int accepted = neigh.fill(
                    new FluidStack(ModFluids.WOOD_GAS_STILL.get(), want),
                    IFluidHandler.FluidAction.SIMULATE
            );
            if (accepted > 0) {
                woodGasTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                neigh.fill(
                        new FluidStack(ModFluids.WOOD_GAS_STILL.get(), accepted),
                        IFluidHandler.FluidAction.EXECUTE
                );
            }
        }
    }

    private boolean canCook() {
        ItemStack split = itemHandler.getStackInSlot(SPLIT_SLOT);
        ItemStack log   = itemHandler.getStackInSlot(LOG_SLOT);
        ItemStack fA    = itemHandler.getStackInSlot(FILTER_SLOTA);
        ItemStack fB    = itemHandler.getStackInSlot(FILTER_SLOTB);

        boolean validLog = log.is(Items.OAK_LOG)
                || log.is(Items.BIRCH_LOG)
                || log.is(Items.SPRUCE_LOG);

        return formed
                && !split.isEmpty()
                && split.getItem() == ModItems.SPLIT_PINE_LOGS.get()
                && validLog
                && !fA.isEmpty() && !fB.isEmpty()
                && fA.getDamageValue() + 2 < fA.getMaxDamage()
                && fB.getDamageValue() + 2 < fB.getMaxDamage();
    }

    private void doCook() {
        itemHandler.extractItem(SPLIT_SLOT, 1, false);
        itemHandler.extractItem(LOG_SLOT,   1, false);

        woodGasTank.fill(
                new FluidStack(ModFluids.WOOD_GAS_STILL.get(), WOOD_GAS_PER_COOK),
                IFluidHandler.FluidAction.EXECUTE
        );

        if (level instanceof ServerLevel server) {
            ItemStack fA = itemHandler.getStackInSlot(FILTER_SLOTA);
            ItemStack fB = itemHandler.getStackInSlot(FILTER_SLOTB);

            fA.hurtAndBreak(2, server, (ServerPlayer)null, broken -> {});
            fB.hurtAndBreak(2, server, (ServerPlayer)null, broken -> {});

            itemHandler.setStackInSlot(FILTER_SLOTA, fA);
            itemHandler.setStackInSlot(FILTER_SLOTB, fB);
        }
    }

    private boolean onContentsChanged(FluidStack ignored) {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.boulanger.wood_gasifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (!isFormed()) {
            player.displayClientMessage(Component.literal("Gasifier incomplete!"), true);
            return null;
        }
        return new WoodGasifierMenu(id, inv, this, data);
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return woodGasTank;
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("formed", formed);
        tag.put("wood_gasifier.inventory", itemHandler.serializeNBT(regs));
        tag.putInt("wood_gasifier.burn_time", burnTime);
        tag.put("wood_gasifier.fluid", woodGasTank.writeToNBT(regs, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("formed")) {
            setFormed(tag.getBoolean("formed"));
        }
        if (tag.contains("wood_gasifier.inventory")) {
            tag.put("inventory", tag.get("wood_gasifier.inventory"));
        }
        itemHandler.deserializeNBT(regs, tag.getCompound("wood_gasifier.inventory"));
        burnTime = tag.getInt("wood_gasifier.burn_time");
        woodGasTank.readFromNBT(regs, tag.getCompound("wood_gasifier.fluid"));
    }

    public boolean tryFormOrDismantle() {
        if (getSlavePositions().isEmpty()) {
            return formStructure();
        } else {
            dismantleStructure();
            return false;
        }
    }

    private boolean formStructure() {
        if (level == null) return false;
        Direction face = getBlockState().getValue(WoodGasifierBlock.FACING);

        BlockPos[] local = {
                new BlockPos(-1, 0, 0),
                new BlockPos( 0, 1, 0),
                new BlockPos(-1, 1, 0)
        };
        Arrays.stream(local)
                .map(off -> rotateOffset(off, face))
                .forEach(off -> {
                    BlockPos p = worldPosition.offset(off);
                    var te = level.getBlockEntity(p);
                    if (!(te instanceof AbstractMultiblockSlaveEntity)) {
                        LOGGER.debug("[Gasifier] no slave at {}", p);
                        throw new IllegalStateException("Missing slave for multiblock");
                    }
                    var slave = (AbstractMultiblockSlaveEntity) te;
                    slave.setMasterPos(worldPosition);
                    addSlave(p);
                    BlockState old = level.getBlockState(p);
                    level.setBlock(p,
                            old.setValue(WoodGasifierSlaveBlock.HIDDEN, true),
                            3
                    );
                });

        setFormed(true);
        return true;
    }

    private BlockPos rotateOffset(BlockPos local, Direction face) {
        return switch (face) {
            case EAST  -> new BlockPos(-local.getZ(), local.getY(),  local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case WEST  -> new BlockPos( local.getZ(), local.getY(), -local.getX());
            default    -> local;
        };
    }

    private void dismantleStructure() {
        getSlavePositions().forEach(slavePos -> {
            BlockState old = level.getBlockState(slavePos);
            level.setBlock(slavePos,
                    old.setValue(WoodGasifierSlaveBlock.HIDDEN, false),
                    3
            );
            var te = level.getBlockEntity(slavePos);
            if (te instanceof AbstractMultiblockSlaveEntity slave) {
                slave.setMasterPos(null);
            }
            removeSlave(slavePos);
        });
        setFormed(false);
        setChanged();
    }
}
