package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.boulangermod.boulanger.item.BreadType;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class BreadTypeComponent {
    private BreadType type;

    // No‐arg constructor: default to BAGUETTE (or null if you prefer)
    public BreadTypeComponent() {
        this.type = BreadType.BAGUETTE;
    }

    // Used by the data‐driven CODEC
    public BreadTypeComponent(BreadType type) {
        this.type = type;
    }

    public BreadType getType() {
        return type;
    }

    public void setType(BreadType type) {
        this.type = type;
    }

    // === JSON / data‐driven codec ===
    // Delegate entirely to BreadType.CODEC
    public static final Codec<BreadTypeComponent> CODEC =
            BreadType.CODEC.xmap(
                    BreadTypeComponent::new,    // wrap enum in component
                    BreadTypeComponent::getType // extract enum for serialization
            );

    // === network sync codec ===
    // Delegate to BreadType.STREAM_CODEC
    public static final StreamCodec<RegistryFriendlyByteBuf, BreadTypeComponent> STREAM_CODEC =
            StreamCodec.composite(
                    BreadType.STREAM_CODEC,      // underlying enum stream codec
                    BreadTypeComponent::getType, // how to extract the enum
                    BreadTypeComponent::new      // how to rebuild component from enum
            );
}
