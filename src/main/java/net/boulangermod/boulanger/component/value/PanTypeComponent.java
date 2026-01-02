package net.boulangermod.boulanger.component.value;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.boulangermod.boulanger.content.pan.PanType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record PanTypeComponent(PanType type) {

    public PanTypeComponent {
        Objects.requireNonNull(type, "type");
    }

    public static PanTypeComponent of(PanType type) {
        return new PanTypeComponent(type);
    }

    public static PanTypeComponent ofId(String raw) {
        PanType t = PanType.byId(raw);
        if (t == null) throw new IllegalArgumentException("Unknown PanType id: " + raw);
        return new PanTypeComponent(t);
    }

    public String idString() { return type.idString(); }
    public int emptyModelIndex() { return type.emptyModelIndex(); }
    public int fullModelIndex()  { return type.fullModelIndex(); }
    public int modelIndex()      { return emptyModelIndex(); }

    public static final Codec<PanTypeComponent> CODEC =
            PanType.CODEC.xmap(PanTypeComponent::new, PanTypeComponent::type);

    public static final StreamCodec<ByteBuf, PanTypeComponent> STREAM_CODEC =
            PanType.STREAM_CODEC.map(PanTypeComponent::new, PanTypeComponent::type);

    public static final StreamCodec<RegistryFriendlyByteBuf, PanTypeComponent> STREAM_CODEC_RF =
            STREAM_CODEC.cast();
}
