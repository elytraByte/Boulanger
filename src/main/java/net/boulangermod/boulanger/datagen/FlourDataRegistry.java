package org.l3e.boulanger.datagen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import org.l3e.boulanger.Boulanger;
import org.l3e.boulanger.item.FlourItem;

public class FlourDataRegistry {

    public static final ResourceKey<Registry<FlourItem.FlourData>> FLOUR_DATA_REGISTRY_KEY =
            ResourceKey.create(
//
//                    net.minecraft.core.registries.Registries.,
//
//                    new ResourceLocation(Boulanger.MODID, "flour_data")
            );
    // 2) Create a Codec for FlourData (example)
    public static final Codec<FlourItem.FlourData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(FlourItem.FlourData::getName),
            Codec.DOUBLE.fieldOf("proteinContent").forGetter(FlourItem.FlourData::getProteinContent),
            Codec.DOUBLE.fieldOf("ashContent").forGetter(FlourItem.FlourData::getAshContent),
            Codec.INT.fieldOf("weightGrams").forGetter(FlourItem.FlourData::getWeightGrams),
            Codec.STRING.fieldOf("textureName").forGetter(FlourItem.FlourData::getTextureName),
            Codec.STRING.optionalFieldOf("description", "").forGetter(FlourItem.FlourData::getDescription)
    ).apply(instance, FlourItem.FlourData::new));
}
