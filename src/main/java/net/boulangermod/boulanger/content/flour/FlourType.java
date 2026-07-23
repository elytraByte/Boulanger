package net.boulangermod.boulanger.content.flour;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record FlourType(String id, float ash, float protein, int modelIndex, long unitMg) {

    public static final Codec<FlourType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(FlourType::id),
            Codec.FLOAT.fieldOf("ash").forGetter(FlourType::ash),
            Codec.FLOAT.fieldOf("protein").forGetter(FlourType::protein),
            Codec.INT.fieldOf("modelIndex").forGetter(FlourType::modelIndex),
            Codec.LONG.fieldOf("unitMg").forGetter(FlourType::unitMg)
    ).apply(instance, FlourType::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlourType> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, FlourType::id,
                    StreamCodecsCompat.FLOAT, FlourType::ash,
                    StreamCodecsCompat.FLOAT, FlourType::protein,
                    StreamCodecsCompat.INT, FlourType::modelIndex,
                    StreamCodecsCompat.LONG, FlourType::unitMg,
                    FlourType::new
            );

    public FlourType withUnitMg(long newUnitMg) {
        return new FlourType(id, ash, protein, modelIndex, newUnitMg);
    }

    public long totalMilligrams(long count) {
        return unitMg *  count;
    }

    public long totalMilligrams(ItemStack stack) {
        return totalMilligrams(stack.getCount());
    }

    public double totalGrams(long count) {
        return totalMilligrams(count) / 1000.0;
    }

    public double totalGrams(ItemStack stack) {
        return totalGrams(stack.getCount());
    }
}
