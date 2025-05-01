package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.fluid.ModFluids;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.WoodGasifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class WoodGasifierBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SPLIT_SLOT   = 0;
    private static final int LOG_SLOT     = 1;
    private static final int FILTER_SLOTA = 2;
    private static final int FILTER_SLOTB = 3;

    public static final int BURN_TIME_PER_LOG = 300;
    private static final int WOOD_GAS_PER_COOK = 1000;
    private static final int FLUID_TRANSFER = 1000;

    private final ItemStackHandler handler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final FluidTank woodGasTank = new FluidTank(8_000, this::onContentsChanged);
    private int burnTime = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int idx) {
            return switch (idx) {
                case 0 -> burnTime;
                case 1 -> woodGasTank.getFluidAmount();
                case 2 -> woodGasTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int idx, int val) {
            if (idx == 0) {
                burnTime = val;
            } else if (idx == 1) {
                FluidStack current = woodGasTank.getFluid();
                if (current.isEmpty()) {
                    woodGasTank.setFluid(new FluidStack(ModFluids.WOOD_GAS_STILL.get(), val));
                } else {
                    woodGasTank.setFluid(current.copyWithAmount(val));
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public WoodGasifierBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.WOOD_GASIFIER_BE.get(), pos, st);
    }

    /** wired up via createTickerHelper in your WoodGasifierBlock */
    public static void ticker(Level level, BlockPos pos, BlockState state, WoodGasifierBlockEntity be) {
        if (level.isClientSide) return;
        LOGGER.debug("[Gasifier] tick @ {}: burnTime={}, tank={}/{}mB",
                pos, be.burnTime, be.woodGasTank.getFluidAmount(), be.woodGasTank.getCapacity());
        be.tickServer(level, pos);
    }

    /** all server-side logic */
    /** all server-side logic */
    private void tickServer(Level level, BlockPos pos) {
        // 1) produce wood-gas if able
        if (burnTime <= 0 && canCook()) {
            doCook();
            burnTime = BURN_TIME_PER_LOG;
        }
        if (burnTime > 0) burnTime--;

        // 2) push wood-gas to any adjacent handler
        // …
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);

            // actually grab the handler into `neigh`:
            IFluidHandler neigh = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    neighborPos,
                    dir.getOpposite()
            );
            LOGGER.debug("[Gasifier] neighbor {} handler? {}", neighborPos, neigh != null);
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
                LOGGER.debug("[Gasifier] pushed {} mB to handler at {}", accepted, neighborPos);
            }
        }

    }


    private boolean canCook() {
        ItemStack split = handler.getStackInSlot(SPLIT_SLOT);
        ItemStack log   = handler.getStackInSlot(LOG_SLOT);
        ItemStack fA    = handler.getStackInSlot(FILTER_SLOTA);
        ItemStack fB    = handler.getStackInSlot(FILTER_SLOTB);

        boolean validLog =
                log.is(Items.OAK_LOG) ||
                        log.is(Items.BIRCH_LOG) ||
                        log.is(Items.SPRUCE_LOG);

        return !split.isEmpty()
                && split.getItem() == ModItems.SPLIT_PINE_LOGS.get()
                && validLog
                && !fA.isEmpty() && !fB.isEmpty()
                && fA.getDamageValue() + 2 < fA.getMaxDamage()
                && fB.getDamageValue() + 2 < fB.getMaxDamage();
    }

    private void doCook() {
        handler.extractItem(SPLIT_SLOT, 1, false);
        handler.extractItem(LOG_SLOT,   1, false);

        woodGasTank.fill(
                new FluidStack(ModFluids.WOOD_GAS_STILL.get(), WOOD_GAS_PER_COOK),
                IFluidHandler.FluidAction.EXECUTE
        );

        // correctly refer to 'level'
        if (level instanceof ServerLevel server) {
            ItemStack fA = handler.getStackInSlot(FILTER_SLOTA);
            ItemStack fB = handler.getStackInSlot(FILTER_SLOTB);

            fA.hurtAndBreak(2, server, (ServerPlayer)null, broken -> {});
            fB.hurtAndBreak(2, server, (ServerPlayer)null, broken -> {});

            handler.setStackInSlot(FILTER_SLOTA, fA);
            handler.setStackInSlot(FILTER_SLOTB, fB);
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
        return new WoodGasifierMenu(id, inv, this, data);
    }

    /** expose tank for piping/UI */
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return woodGasTank;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return handler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("wood_gasifier.inventory", handler.serializeNBT(regs));
        tag.putInt("wood_gasifier.burn_time", burnTime);
        tag.put("wood_gasifier.fluid", woodGasTank.writeToNBT(regs, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        handler.deserializeNBT(regs, tag.getCompound("wood_gasifier.inventory"));
        burnTime = tag.getInt("wood_gasifier.burn_time");
        woodGasTank.readFromNBT(regs, tag.getCompound("wood_gasifier.fluid"));
    }




}
