package net.boulangermod.boulanger.content.pan;

import net.boulangermod.boulanger.component.ModDataComponentTypes;
import net.boulangermod.boulanger.component.value.PanTypeComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import javax.annotation.Nullable;

public final class PanTypes {
    private PanTypes() {}

    @Nullable
    public static PanType fromStack(ItemStack stack) {
        PanTypeComponent comp = stack.get(ModDataComponentTypes.PAN_TYPE.get());
        if (comp != null) return comp.type();

        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            int v = cmd.value();
            for (PanType t : PanType.values()) {
                if (t.emptyModelIndex() == v || t.fullModelIndex() == v) return t;
            }
        }
        return null;
    }
}
