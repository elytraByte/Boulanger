// StoneMillBlockEntity.java
package net.boulangermod.boulanger.block.entity;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.block.AbstractProcessingBlock;
import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.FlourItemType;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.screen.StoneMillBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class StoneMillBlockEntity extends AbstractPoweredBlockEntity
        implements AbstractProcessingBlock.Tickable, MenuProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAX_MILL_TIME     = 200;   // ticks for one operation
    private static final int FE_COST_PER_TICK  = 20;    // FE/t while milling
    private static final int FE_CAPACITY       = 20000; // total FE buffer
    private static final int FE_MAX_RECEIVE    = 200;   // FE/t accepted

    private int millProgress = 0;
    private boolean milling = false;
    private int logCooldown = 0; // throttle repeated tick logs

    public StoneMillBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.STONE_MILL_BE.get(),
                pos, state,
                /*slots=*/2,               // 0: input, 1: output
                FE_CAPACITY,
                FE_MAX_RECEIVE,
                FE_COST_PER_TICK
        );
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energy; // allow all sides for now
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.STONE_MILL_BE.get();
    }

    // --- Helpers ---
    private static String idOf(ItemStack stack) {
        if (stack.isEmpty()) return "(empty)";
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : stack.getItem().toString();
    }

    // --- Machine state ---
    private boolean canMill() {
        ItemStack in  = itemHandler.getStackInSlot(0);
        ItemStack out = itemHandler.getStackInSlot(1);

        if (in.isEmpty()) {
            return false;
        }
        if (!in.is(Items.WHEAT)) {
            return false;
        }

        if (out.isEmpty()) {
            return true;
        }

        if (out.getItem() == ModItems.FLOUR_ITEM.get()) {
            FlourType target   = FlourItemType.WHOLE_WHEAT_FLOUR.toFlourType();
            FlourType existing = out.get(ModDataComponentTypes.FLOUR_TYPE.get());
            boolean sameType   = existing != null && existing.equals(target);
            return true;
        }

        return false;
    }

    private void craftResult() {
        ItemStack in  = itemHandler.getStackInSlot(0);
        ItemStack out = itemHandler.getStackInSlot(1);
        if (in.isEmpty() || !in.is(Items.WHEAT)) return;

        ItemStack result = new ItemStack(ModItems.FLOUR_ITEM.get(), 1);
        result.set(ModDataComponentTypes.FLOUR_TYPE.get(), FlourItemType.WHOLE_WHEAT_FLOUR.toFlourType());

        if (out.isEmpty()) {
            itemHandler.setStackInSlot(1, result);
        } else {
            out.grow(1);
        }
        in.shrink(1);
    }

    public void resetMilling() {
        millProgress = 0;
        milling = false;
        setChanged();
    }

    public int getMixProgress() { return millProgress; }
    public boolean isMilling() { return milling; }
    public static int getMaxMixTime() { return MAX_MILL_TIME; }

    // --- Persistence ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MillProgress", millProgress);
        tag.putBoolean("Milling", milling);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        millProgress = tag.getInt("MillProgress");
        milling = tag.getBoolean("Milling");
    }

    // --- MenuProvider ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("stone_mill.boulanger");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StoneMillBlockMenu(id, inv, this);
    }

    // --- Ticking ---
    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        // 0) Auto-pull FE from neighbors (up to FE_MAX_RECEIVE)
        {
            int movedTotal = 0;
            int free = energy.getMaxEnergyStored() - energy.getEnergyStored();
            if (free > 0 && FE_MAX_RECEIVE > 0) {
                for (Direction dir : Direction.values()) {
                    if (movedTotal >= FE_MAX_RECEIVE) break;

                    BlockPos npos = pos.relative(dir);
                    var src = level.getCapability(
                            net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                            npos,
                            dir.getOpposite()
                    );
                    if (src == null) continue;

                    int want = Math.min(FE_MAX_RECEIVE - movedTotal, free);
                    int canExtract = src.extractEnergy(want, true);
                    if (canExtract <= 0) continue;

                    int received = energy.receiveEnergy(canExtract, false);
                    if (received <= 0) continue;

                    int actuallyExtracted = src.extractEnergy(received, false);
                    if (actuallyExtracted > 0) {
                        movedTotal += actuallyExtracted;
                        free      -= actuallyExtracted;
                    }
                }
                if (movedTotal > 0) {
                    dirty = true;
                }
            }
        }

        // 1) Throttled heartbeat (once per second)
        if (logCooldown-- <= 0) {
            ItemStack in  = itemHandler.getStackInSlot(0);
            ItemStack out = itemHandler.getStackInSlot(1);
            logCooldown = 20; // ~1s
        }

        // 2) If idle, try to start
        if (!milling) {
            boolean can = canMill();
            boolean hasPower = hasPowerForTick();

            if (can && hasPower) {
                milling = true;
                dirty = true;
            }
        }

        // 3) If running, consume power & progress
        if (milling) {
            // If we dipped under the per-tick cost, try to pull again right now
            if (!hasPowerForTick()) {
                int movedTotal = 0;
                int free = energy.getMaxEnergyStored() - energy.getEnergyStored();
                if (free > 0 && FE_MAX_RECEIVE > 0) {
                    for (Direction dir : Direction.values()) {
                        if (movedTotal >= FE_MAX_RECEIVE) break;

                        BlockPos npos = pos.relative(dir);
                        var src = level.getCapability(
                                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                                npos,
                                dir.getOpposite()
                        );
                        if (src == null) continue;

                        int want = Math.min(FE_MAX_RECEIVE - movedTotal, free);
                        int canExtract = src.extractEnergy(want, true);
                        if (canExtract <= 0) continue;

                        int received = energy.receiveEnergy(canExtract, false);
                        if (received <= 0) continue;

                        int actuallyExtracted = src.extractEnergy(received, false);
                        if (actuallyExtracted > 0) {
                            movedTotal += actuallyExtracted;
                            free      -= actuallyExtracted;
                        }
                    }
                    if (movedTotal > 0) {
                        dirty = true;
                    }
                }
            }

            if (!tryConsumePowerForTick()) {
                milling = false; // pause until power returns
                dirty = true;
            } else {
                millProgress++;
                if ((millProgress % 20) == 0) {
                }

                if (millProgress >= MAX_MILL_TIME) {
                    if (canMill()) {
                        craftResult();
                    } else {
                    }
                    resetMilling();
                    dirty = true;
                }
            }
        }

        if (dirty) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

}