package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

public enum StepType {
    AUTOLYSE("autolyse"),
    PROOF("proof"),
    PUNCHDOWN("punchdown"),
    SHAPE("shape"),
    DIVIDE("divide"),
    FINAL_PROOF("final_proof"),
    BAKE("bake");

    private final String id;

    StepType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static StepType byId(String raw) {
        if (raw == null) return null;
        String s = raw.toLowerCase(Locale.ROOT);

        for (StepType t : values()) {
            if (t.id.equals(s)) return t;
        }

        try {
            return StepType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static final Codec<StepType> CODEC =
            Codec.STRING.comapFlatMap(s -> {
                StepType t = byId(s);
                return t != null
                        ? DataResult.success(t)
                        : DataResult.error(() -> "Unknown StepType: " + s);
            }, StepType::id);

    public static final StreamCodec<ByteBuf, StepType> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    s -> {
                        StepType t = byId(s);
                        if (t == null) throw new IllegalArgumentException("Unknown StepType: " + s);
                        return t;
                    },
                    StepType::id
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, StepType> STREAM_CODEC_RF =
            STREAM_CODEC.cast();
}
