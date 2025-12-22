package net.boulangermod.boulanger.recipe; // <-- keep your existing package

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

// import your enums/classes
import net.boulangermod.boulanger.item.PortionKind;

/** Lightweight value object describing a single pan load entry. */
public record PanLoadComponent(ResourceLocation recipeId, PortionKind portionKind, int count) {

    // -------- JSON / data codec --------
    public static final MapCodec<PanLoadComponent> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("recipe").forGetter(PanLoadComponent::recipeId),
            PortionKind.CODEC.fieldOf("portion_kind").forGetter(PanLoadComponent::portionKind),
            Codec.INT.fieldOf("count").forGetter(PanLoadComponent::count)
    ).apply(i, PanLoadComponent::new));

    // -------- Network codec (ByteBuf-typed) --------
    public static final StreamCodec<ByteBuf, PanLoadComponent> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PanLoadComponent::recipeId,
            PortionKind.STREAM_CODEC,     PanLoadComponent::portionKind,
            ByteBufCodecs.VAR_INT,        PanLoadComponent::count,
            PanLoadComponent::new
    );
}
