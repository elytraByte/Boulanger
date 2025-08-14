        package net.boulangermod.boulanger.screen;

        import com.mojang.blaze3d.systems.RenderSystem;
        import net.boulangermod.boulanger.Boulanger;
        import net.minecraft.client.gui.GuiGraphics;
        import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
        import net.minecraft.network.chat.Component;
        import net.minecraft.resources.ResourceLocation;
        import net.minecraft.world.entity.player.Inventory;

        public class WoodGasifierScreen extends AbstractContainerScreen<WoodGasifierMenu> {
            private static final ResourceLocation TEXTURE =
                    ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/gui/wood_gasifier.png");

            public WoodGasifierScreen(WoodGasifierMenu menu,
                                      Inventory inv,
                                      Component title) {
                super(menu, inv, title);
                this.imageWidth  = 176;
                this.imageHeight = 172;  // <-- match your 172px-tall texture
                // inventoryLabelY is computed in super.init() as imageHeight - 94
            }

            @Override
            protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
                RenderSystem.setShaderTexture(0, TEXTURE);
                int x = leftPos, y = topPos;

                // background
                gfx.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

                // burn arrow (guarded too, just in case)
                int maxBurn = menu.getMaxBurnProgress();
                int prog    = maxBurn > 0
                        ? menu.getBurnProgress() * 24 / maxBurn
                        : 0;
                gfx.blit(TEXTURE, x + 56, y + 37, 176, 0, prog + 1, 16);

                // energy bar (guard against zero max)
                int maxEnergy = menu.getMaxEnergyStored();
                int energy    = maxEnergy > 0
                        ? menu.getEnergyStored() * 50 / maxEnergy
                        : 0;
                gfx.blit(TEXTURE,
                        x + 150,
                        y + 15 + (50 - energy),
                        176,
                        16 + (50 - energy),
                        14,
                        energy);
            }


            @Override
            protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
                // Centered main title
                int titleWidth = this.font.width(this.title);
                int titleX     = (this.imageWidth - titleWidth) / 2;
                gfx.drawString(this.font, this.title, titleX, 6, 0x404040, false);

                // Move the “Inventory” label down by 5 pixels
                gfx.drawString(
                        this.font,
                        this.playerInventoryTitle,
                        this.inventoryLabelX,
                        this.inventoryLabelY + 5,   // <-- shifted down 5px
                        0x404040,
                        false
                );


            }


            @Override
            public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
                // dim background
                this.renderBackground(gfx, mouseX, mouseY, partialTicks);
                // draw GUI and slots
                super.render(gfx, mouseX, mouseY, partialTicks);
                // tooltips
                this.renderTooltip(gfx, mouseX, mouseY);
            }
        }
