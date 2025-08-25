package net.boulangermod.boulanger.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.FlourType;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.item.ModItems;
import net.boulangermod.boulanger.network.StartMixingPacket;
import net.boulangermod.boulanger.util.IngredientCategory;
import net.boulangermod.boulanger.util.IngredientStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Locale;

public class MixingBlockScreen extends AbstractContainerScreen<MixingBlockMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/mixer.png");

    // Sidebar layout
    private static final int COLUMNS = 3;
    private static final int ICON_SZ = 16;
    private static final int VERT_SP = 18; // vertical step between rows
    private static final int COL_SP  = 5; // horizontal gap between columns
    private static final int PADDING = 10;  // inner padding of the panel

    // Overlay label
    private static final int  LABEL_COLOR = 0xFFAA00; // gold
    private static final boolean LABEL_SHADOW = true;
    private static final float LABEL_SCALE = 0.8f; // <— make smaller/bigger here

    public MixingBlockScreen(MixingBlockMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 72;
        this.titleLabelX     = (this.imageWidth - this.font.width(this.title)) / 2;

        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;
        int btnW = 40, btnH = 18;

        Button mixBtn = Button.builder(Component.literal("Mix"), b ->
                        StartMixingPacket.sendFromClient(menu.getBlockEntity().getBlockPos())
                )
                .bounds(x0 + this.imageWidth/2 - btnW/2, y0 + 60, btnW, btnH)
                .build();

        this.addRenderableWidget(mixBtn);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        // 1) draw main GUI scaled 1/4
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f,1f,1f,1f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale(0.25f, 0.25f, 1f);
        g.blit(GUI_TEXTURE, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
        g.pose().popPose();

        // 2) sidebar geometry (left of the main GUI)
        int count = menu.getIngredientCount();

        // measure widest label (scaled) so columns never collide
        int maxLabelW = 0;
        for (int i = 0; i < count; i++) {
            maxLabelW = Math.max(maxLabelW, this.font.width(shortWeight(menu.getIngredient(i).getMilligrams())));
        }
        int scaledMaxLabelW = (int)Math.ceil(maxLabelW * LABEL_SCALE);
        int colStride = Math.max(ICON_SZ, scaledMaxLabelW) + COL_SP;

        int iconsAreaW = ICON_SZ + (COLUMNS - 1) * colStride;
        int panelW     = PADDING * 2 + iconsAreaW;           // minimal width: no unused gray
        int px1        = x0;                                  // flush to GUI's left edge
        int px0        = px1 - panelW;
        int py0        = y0;
        int py1        = y0 + this.imageHeight;

        // draw panel background (8b8b8b)
        g.fill(px0, py0, px1, py1, 0xFF8B8B8B);

        // 3) render icons, then overlay labels (labels drawn AFTER, with higher Z)
        for (int i = 0; i < count; i++) {
            IngredientStack ingr = menu.getIngredient(i);
            ItemStack display    = buildDisplayStack(ingr);

            int col = i % COLUMNS;
            int row = i / COLUMNS;

            int iconX = px0 + PADDING + col * colStride;
            int iconY = py0 + PADDING + row * VERT_SP;

            // item sprite first
            g.renderItem(display, iconX, iconY);

            // gold overlay text ON TOP (bottom-right inside the 16x16), scaled smaller
            String label = shortWeight(ingr.getMilligrams());

            int tw = this.font.width(label);
            int th = this.font.lineHeight;

            float scaledTw = tw * LABEL_SCALE;
            float scaledTh = th * LABEL_SCALE;

            float tx = iconX + ICON_SZ - scaledTw;
            float ty = iconY + ICON_SZ - scaledTh;

            // Raise Z so it cannot end up underneath the item sprite
            g.pose().pushPose();
            g.pose().translate(tx, ty, 200.0f);  // high Z
            g.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0f);
            g.drawString(this.font, label, 0, 0, LABEL_COLOR, LABEL_SHADOW);
            g.pose().popPose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        this.renderBackground(g, mx, my, dt);
        super.render(g, mx, my, dt);

        // tooltip hit-test (match geometry from renderBg)
        int x0 = (this.width  - this.imageWidth)  / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        int count = menu.getIngredientCount();

        int maxLabelW = 0;
        for (int i = 0; i < count; i++) {
            maxLabelW = Math.max(maxLabelW, this.font.width(shortWeight(menu.getIngredient(i).getMilligrams())));
        }
        int scaledMaxLabelW = (int)Math.ceil(maxLabelW * LABEL_SCALE);
        int colStride = Math.max(ICON_SZ, scaledMaxLabelW) + COL_SP;

        int iconsAreaW = ICON_SZ + (COLUMNS - 1) * colStride;
        int panelW     = PADDING * 2 + iconsAreaW;
        int px1        = x0;
        int px0        = px1 - panelW;

        for (int i = 0; i < count; i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;

            int iconX = px0 + PADDING + col * colStride;
            int iconY = y0 + PADDING + row * VERT_SP;

            if (mx >= iconX && mx < iconX + ICON_SZ && my >= iconY && my < iconY + ICON_SZ) {
                IngredientStack ingr = menu.getIngredient(i);
                ItemStack display    = buildDisplayStack(ingr);

                Component line1 = Component.literal("\u00A7a" + display.getHoverName().getString());
                Component line2 = Component.literal("\u00A7a" + longWeight(ingr.getMilligrams()));
                g.renderComponentTooltip(this.font, List.of(line1, line2), mx, my);
                break;
            }
        }

        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, 72, 0x404040, false);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────────────────

    /** Build a per-ingredient display stack so flour shows its correct variant model. */
    private ItemStack buildDisplayStack(IngredientStack ingr) {
        // FLOUR: prefer concrete flour registry item by FlourType id; else base flour with model data
        if (ingr.getCategory() == IngredientCategory.FLOUR) {
            FlourType ft = ingr.getFlourType();

            ItemStack display = ItemStack.EMPTY;

            if (ft != null) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, ft.getId());
                Item maybe = BuiltInRegistries.ITEM.get(id);
                if (maybe != Items.AIR) {
                    display = new ItemStack(maybe);
                }
            }

            if (display.isEmpty() && ModItems.FLOUR_ITEM != null) {
                display = new ItemStack(ModItems.FLOUR_ITEM.get());
            }

            if (display.isEmpty() && ingr.getActualItem() != null) {
                display = new ItemStack(ingr.getActualItem());
            }

            if (!display.isEmpty() && ft != null) {
                display.set(ModDataComponentTypes.FLOUR_TYPE.get(), ft);
                display.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ft.getModelIndex()));
            }
            return display;
        }

        // NON-FLOUR: show the actual ingredient item the server stored
        Item actual = ingr.getActualItem();
        if (actual != null && actual != Items.AIR) {
            return new ItemStack(actual);
        }

        // last resort (shouldn't happen now)
        ItemStack bowl = ingr.getBowlStack();
        return (bowl == null) ? ItemStack.EMPTY : bowl.copy();
    }

    private static String shortWeight(int milligrams) {
        if (milligrams < 1000) return milligrams + " mg";
        double g = milligrams / 1000.0;
        return String.format(Locale.ROOT, "%.1f g", g);
    }

    private static String longWeight(int milligrams) {
        double g = milligrams / 1000.0;
        return String.format(Locale.ROOT, "%d mg (%.3f g)", milligrams, g);
    }
}
