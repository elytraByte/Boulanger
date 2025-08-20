package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.entity.WoodGasEngineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Zero-slot menu that just syncs progress/levels from the WoodGasEngineBlockEntity.
 */
public class WoodGasEngineBlockMenu extends AbstractContainerMenu {
    public final Level level;
    public final BlockPos pos;
    @Nullable public final WoodGasEngineBlockEntity be;

    // Client-side copies populated via DataSlots
    private int burnProgress;     // ticks progressed
    private int burnTotal;        // total ticks per cycle
    private int gasAmount;        // mB
    private int gasCapacity;      // mB

    // ----- Constructors -----
    // Client
    public WoodGasEngineBlockMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (WoodGasEngineBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    // Server
    public WoodGasEngineBlockMenu(int id, Inventory inv, @Nullable WoodGasEngineBlockEntity be) {
        super(ModMenuTypes.WOODGAS_ENGINE_MENU.get(), id);
        this.level = inv.player.level();
        this.pos   = be != null ? be.getBlockPos() : BlockPos.ZERO;
        this.be    = be;

        // --- Data sync channels ---
        // burnProgress
        addDataSlot(new DataSlot() {
            @Override public int get() { return be != null ? be.getBurnProgress() : burnProgress; }
            @Override public void set(int v) { burnProgress = v; }
        });
        // burnTotal
        addDataSlot(new DataSlot() {
            @Override public int get() { return be != null ? be.getBurnTotal() : burnTotal; }
            @Override public void set(int v) { burnTotal = v; }
        });
        // gasAmount
        addDataSlot(new DataSlot() {
            @Override public int get() { return be != null ? be.getGasAmount() : gasAmount; }
            @Override public void set(int v) { gasAmount = v; }
        });
        // gasCapacity
        addDataSlot(new DataSlot() {
            @Override public int get() { return be != null ? be.getGasCapacity() : gasCapacity; }
            @Override public void set(int v) { gasCapacity = v; }
        });


    }

    // ----- Accessors used by the Screen -----
    public int getBurnProgress() { return burnProgress; }
    public int getBurnTotal()    { return Math.max(burnTotal, 1); } // avoid div/0
    public int getGasAmount()    { return gasAmount; }
    public int getGasCapacity()  { return Math.max(gasCapacity, 1); }

    public float getBurnPercent() {
        return Math.min(1f, (float) getBurnProgress() / (float) getBurnTotal());
    }
    public float getGasPercent() {
        return Math.min(1f, (float) getGasAmount() / (float) getGasCapacity());
    }

    // ----- Container plumbing -----
    @Override
    public boolean stillValid(Player player) {
        return be != null && player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    // No slots to quick-move
    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int idx) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
