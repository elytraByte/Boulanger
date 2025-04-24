// HefferRenderer.java
package net.boulangermod.boulanger.entity;

import net.boulangermod.boulanger.Boulanger;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class HolsteinFriesianCowRenderer extends CowRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "textures/entity/heffer.png");

    public HolsteinFriesianCowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Cow entity) {
        return TEXTURE;
    }
}
