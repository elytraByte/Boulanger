package net.boulangermod.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public class FoodAdditiveComponent {
    private final String id;
    private final WeightComponent weight;  // Use WeightComponent

    public FoodAdditiveComponent(String id, WeightComponent weight) {
        this.id = id;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public WeightComponent getWeightComponent() {
        return weight;
    }

    // Convenience method that returns the float weight
    public float getWeight() {
        return weight.getWeight();
    }

    // Codec for persistent storage using the WeightComponent's codec
    public static final Codec<FoodAdditiveComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(FoodAdditiveComponent::getId),
                    WeightComponent.CODEC.fieldOf("weight").forGetter(FoodAdditiveComponent::getWeightComponent)
            ).apply(instance, FoodAdditiveComponent::new)
    );

    // Network synchronization codec
    public static final StreamCodec<RegistryFriendlyByteBuf, FoodAdditiveComponent> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.STRING, FoodAdditiveComponent::getId,
                    WeightComponent.STREAM_CODEC, FoodAdditiveComponent::getWeightComponent,
                    FoodAdditiveComponent::new
            );

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FoodAdditiveComponent)) return false;
        FoodAdditiveComponent that = (FoodAdditiveComponent) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, weight);
    }

}
