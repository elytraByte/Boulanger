package net.boulangermod.boulanger.screen;

import net.boulangermod.boulanger.block.entity.WoodGasEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Zero-slot menu that syncs burn progress and gas levels from the WoodGasEngineBlockEntity.
 */
public class WoodGasEngineBlockMenu extends AbstractContainerMenu {
    public final Level level;
    public final BlockPos pos;
    @Nullable public final WoodGasEngineBlockEntity be;

    // Optional indices if you ever switch to ContainerData
    public static final int IDX_BURN_REMAIN = 0;
    public static final int IDX_BURN_TOTAL  = 1;
    public static final int IDX_GAS_AMT     = 2;
    public static final int IDX_GAS_CAP     = 3;

    // Client-side mirrors populated by DataSlots
    private int burnProgress; // ticks progressed (0 → burnTotal)
    private int burnTotal;    // total ticks per cycle
    private int gasAmount;    // mB
    private int gasCapacity;  // mB
    private int litFlag;      // 1 = burning, 0 = not

    // ───────────────────────────────────────────────────────────────────────────
    // Constructors
    // ───────────────────────────────────────────────────────────────────────────

    // Client: resolve BE from pos in the buffer
    public WoodGasEngineBlockMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (WoodGasEngineBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    // Server: BE provided by factory
    public WoodGasEngineBlockMenu(int id, Inventory inv, @Nullable WoodGasEngineBlockEntity be) {
        super(ModMenuTypes.WOODGAS_ENGINE_MENU.get(), id);
        this.level = inv.player.level();
        this.be    = be;
        this.pos   = (be != null) ? be.getBlockPos() : BlockPos.ZERO;

        // --- Data sync channels (server → client) ---
        // burnProgress (ticks progressed)
        addDataSlot(new DataSlot() {
            @Override public int get() { return (be != null) ? be.getBurnProgress() : burnProgress; }
            @Override public void set(int v) { burnProgress = v; }
        });

        // burnTotal
        addDataSlot(new DataSlot() {
            @Override public int get() { return (be != null) ? be.getBurnTotal() : burnTotal; }
            @Override public void set(int v) { burnTotal = v; }
        });

        // gasAmount (mB)
        addDataSlot(new DataSlot() {
            @Override public int get() { return (be != null) ? be.getGasAmount() : gasAmount; }
            @Override public void set(int v) { gasAmount = v; }
        });

        // gasCapacity (mB)
        addDataSlot(new DataSlot() {
            @Override public int get() { return (be != null) ? be.getGasCapacity() : gasCapacity; }
            @Override public void set(int v) { gasCapacity = v; }
        });

        // isLit flag (authoritative)
        addDataSlot(new DataSlot() {
            @Override public int get() { return (be != null && be.isLit()) ? 1 : 0; }
            @Override public void set(int v) { litFlag = v; }
        });
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Accessors for Screen
    // ───────────────────────────────────────────────────────────────────────────

    public int getBurnProgress() { return burnProgress; }
    public int getBurnTotal()    { return Math.max(burnTotal, 1); } // guard div/0
    public int getGasAmount()    { return gasAmount; }
    public int getGasCapacity()  { return Math.max(gasCapacity, 1); }

    /** 0 → 1 as the current burn cycle progresses. */
    public float getBurnPercent() {
        return Math.min(1f, (float) getBurnProgress() / (float) getBurnTotal());
    }

    /** 0 → 1 fill for the gas gauge. */
    public float getGasPercent() {
        return Math.min(1f, (float) getGasAmount() / (float) getGasCapacity());
    }

    /** Authoritative burning state, synced from BE. */
    public boolean isBurning() {
        return litFlag == 1;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Container plumbing
    // ───────────────────────────────────────────────────────────────────────────

    @Override
    public boolean stillValid(Player player) {
        if (be == null) return false;
        final double dx = player.getX() - (pos.getX() + 0.5);
        final double dy = player.getY() - (pos.getY() + 0.5);
        final double dz = player.getZ() - (pos.getZ() + 0.5);
        return dx*dx + dy*dy + dz*dz <= 64.0; // 8 blocks
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        return ItemStack.EMPTY; // no slots
    }
}
