package org.l3e.boulanger.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.l3e.boulanger.datagen.FlourDataRegistry;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class FlourItem extends Item {

    private final String flourType; // This should match the registry key for your FlourData

    public FlourItem(Properties properties, String flourType) {
        super(properties);
        this.flourType = flourType;
    }

    // Change getFlourData to take a Level so we can query the registry.
    public FlourData getFlourData(Level level) {
        return level.registryAccess()
                .registry(FlourDataRegistry.FLOUR_DATA_REGISTRY_KEY)
                .flatMap(registry -> Optional.ofNullable(registry.get(new net.minecraft.resources.ResourceLocation(flourType))))
                .orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        if (level != null) {
            FlourData data = getFlourData(level);
            if (data != null) {
                tooltipComponents.add(Component.translatable("item.boulanger.flour.tooltip.weight", data.getWeightGrams())
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
                tooltipComponents.add(Component.translatable("item.boulanger.flour.tooltip.protein", data.getProteinContent())
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
                tooltipComponents.add(Component.translatable("item.boulanger.flour.tooltip.ash", data.getAshContent())
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
                if (data.getDescription() != null && !data.getDescription().isEmpty()) {
                    tooltipComponents.add(Component.literal(" ")
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
                    tooltipComponents.add(Component.translatable(data.getDescription())
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFCC80)).withItalic(true)));
                }
            } else {
                tooltipComponents.add(Component.literal("Error: No data found for this flour type.")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000))));
            }
        }
    }

    // Inner class for flour data – note the new constructor for codec decoding.
    public static class FlourData {
        private String name;
        private double proteinContent;
        private double ashContent;
        private int weightGrams;
        private String textureName;
        private String description;

        // Default constructor (if needed)
        public FlourData() { }

        // Constructor used by the Codec
        public FlourData(String name, double proteinContent, double ashContent, int weightGrams, String textureName, String description) {
            this.name = name;
            this.proteinContent = proteinContent;
            this.ashContent = ashContent;
            this.weightGrams = weightGrams;
            this.textureName = textureName;
            this.description = description;
        }

        // Getters
        public String getName() { return name; }
        public double getProteinContent() { return proteinContent; }
        public double getAshContent() { return ashContent; }
        public int getWeightGrams() { return weightGrams; }
        public String getTextureName() { return textureName; }
        public String getDescription() { return description; }

        // Setters if needed...
    }

    // (Other methods remain unchanged.)
}
