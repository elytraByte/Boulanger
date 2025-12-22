package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum StepType {
    AUTOLYSE,
    PROOF,
    PUNCHDOWN,
    SHAPE,
    DIVIDE,
    FINAL_PROOF,
    BAKE;

    public static final Codec<StepType> CODEC = Codec.STRING.xmap(
            StepType::valueOf,
            StepType::name
    );

    public static final StreamCodec<io.netty.buffer.ByteBuf, StepType> STREAM_CODEC =
            ByteBufCodecs.idMapper(
                    (int i) -> StepType.values()[i],
                    StepType::ordinal
            );
}
