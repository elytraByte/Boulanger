package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;


public record FlourType(String type, float ash, float protein, int modelIndex, float weight) {
    public static final Codec<FlourType> CODEC = RecordCodecBuilder.create
            (instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(FlourType::type),
            Codec.FLOAT.fieldOf("ash").forGetter(FlourType::ash),
            Codec.FLOAT.fieldOf("protein").forGetter(FlourType::protein),
            Codec.INT.fieldOf("modelIndex").forGetter(FlourType::modelIndex),
            Codec.FLOAT.fieldOf("weight").forGetter(FlourType::weight)
    ).apply(instance, FlourType::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlourType> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, FlourType::type,
                    StreamCodecsCompat.FLOAT, FlourType::ash,
                    StreamCodecsCompat.FLOAT, FlourType::protein,
                    StreamCodecsCompat.INT, FlourType::modelIndex,
                    StreamCodecsCompat.FLOAT, FlourType::weight,
                    FlourType::new
            );

    public String getId() {
        return type;
    }

    public float getAshContent() {
        return ash;
    }

    public float getProteinContent() {
        return protein;
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public float getWeight() {
        return  weight;
    }

    public FlourType withWeight(float newWeight) {
        return new FlourType(type, ash, protein, modelIndex, newWeight);
    }



}
