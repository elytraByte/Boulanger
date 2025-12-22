// src/main/java/net/boulangermod/boulanger/item/PanType.java
package net.boulangermod.boulanger.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.boulangermod.boulanger.Boulanger;
import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.PanTypeComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PanType {
    // id (string or namespaced), capacity, empty CMD, full CMD, assetKey RL (texture/model stem)
    LOAF(
            "boulanger:loaf",
            1, 1, 2,
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "loaf_pan")
    ),
    BAGUETTE(
            "boulanger:baguette",
            3, 3, 4,
            ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, "baguette_pan")
    );

    private final ResourceLocation id;              // canonical type id (e.g., boulanger:loaf)
    private final int capacity;                     // per-pan capacity for this pan form
    private final int emptyModelIndex;              // CustomModelData index for empty
    private final int fullModelIndex;               // CustomModelData index for full
    private final ResourceLocation assetKey;        // e.g., boulanger:loaf_pan (-> textures/item/loaf_pan.png)

    PanType(String idOrPath, int capacity, int emptyModelIndex, int fullModelIndex, ResourceLocation assetKey) {
        ResourceLocation parsed = ResourceLocation.tryParse(idOrPath);
        this.id = parsed != null ? parsed : ResourceLocation.fromNamespaceAndPath(Boulanger.MODID, idOrPath);
        this.capacity = capacity;
        this.emptyModelIndex = emptyModelIndex;
        this.fullModelIndex = fullModelIndex;
        this.assetKey = assetKey;
    }

    private static final Map<ResourceLocation, PanType> BY_ID =
            Arrays.stream(values()).collect(Collectors.toMap(PanType::getIdRL, Function.identity()));

    /** JSON codec: serialize as the PanType id (ResourceLocation), e.g. "boulanger:loaf". */
    public static final Codec<PanType> CODEC =
            ResourceLocation.CODEC.flatXmap(
                    rl -> {
                        PanType t = BY_ID.get(rl);
                        return t != null
                                ? DataResult.success(t)
                                : DataResult.error(() -> "Unknown PanType id: " + rl);
                    },
                    t -> DataResult.success(t.getIdRL())
            );


    public ResourceLocation getIdRL() { return id; }
    @Deprecated public String getId() { return id.toString(); } // legacy callers
    public int getCapacity() { return capacity; }
    public int getEmptyModelIndex() { return emptyModelIndex; }
    public int getFullModelIndex() { return fullModelIndex; }
    public ResourceLocation getAssetKey() { return assetKey; }   // use in datagen/model/texture resolution

    /** Prefer this to infer PanType from a stack. */
    @Nullable
    public static PanType getId(ItemStack stack) {
        PanTypeComponent comp = stack.get(ModDataComponentTypes.PAN_TYPE.get());
        if (comp != null) {
            for (PanType t : values()) if (t.id.equals(comp.id())) return t;
        }
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            int v = cmd.value();
            for (PanType t : values()) if (t.emptyModelIndex == v || t.fullModelIndex == v) return t;
        }
        return null;
    }

    public static @Nullable PanType byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static java.util.Optional<PanType> byIdOpt(ResourceLocation id) {
        return java.util.Optional.ofNullable(BY_ID.get(id));
    }

    // if useful:
    public static @Nullable PanType byId(String id) {
        return id == null ? null : byId(ResourceLocation.tryParse(id));
    }
}
