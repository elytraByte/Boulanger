package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum StepType {
    AUTOLYSE,
    PROOF,
    PUNCHDOWN,
    SHAPE,
    FINAL_PROOF,
    BAKE;

    public static final Codec<StepType> CODEC = Codec.STRING.xmap(
            StepType::valueOf,
            StepType::name
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StepType> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> buf.writeUtf(value.name()),
                    buf -> StepType.valueOf(buf.readUtf())
            );
}
