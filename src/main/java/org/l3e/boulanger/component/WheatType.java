package org.l3e.boulanger.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

public record WheatType(int type) {

    public static final Codec<WheatType> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.INT.fieldOf("type").forGetter(WheatType::type)).apply(instance, WheatType::new));


    @Override
    public int hashCode() {
        return Objects.hash(this.type);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        } else {
            return obj instanceof WheatType wheatType && this.type == wheatType.type;
        }
    }
}