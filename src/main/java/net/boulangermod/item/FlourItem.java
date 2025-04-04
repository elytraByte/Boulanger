package net.boulangermod.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.boulangermod.datagen.FlourDataRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FlourItem extends Item {

    private final String flourType; // Should match the registry key for your FlourData

    public FlourItem(Properties properties, String flourType) {
        super(properties);
        this.flourType = flourType;
    }

    // Retrieves the FlourData using the provided Level.
    public FlourData getFlourData(@Nullable Level level) {
        if (level == null) return null;
        return level.registryAccess()
                .lookup(FlourDataRegistry.FLOUR_DATA_REGISTRY_KEY)
                .flatMap(registryLookup ->
                        registryLookup.get(ResourceLocation.parse(this.flourType))
                )
                .map(Holder::value)
                .orElse(null);
    }

    // Override with the correct signature: (ItemStack, TooltipContext, List<Component>)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, tooltipContext, tooltipComponents, tooltipFlag);

        // Obtain the client world (Level) from the Minecraft instance.
        Level level = Minecraft.getInstance().level;
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

    // Inner class for flour data.
    public static class FlourData {
        private String name;
        private double proteinContent;
        private double ashContent;
        private int weightGrams;
        private String textureName;
        private String description;

        public FlourData() { }

        public FlourData(String name, double proteinContent, double ashContent, int weightGrams, String textureName, String description) {
            this.name = name;
            this.proteinContent = proteinContent;
            this.ashContent = ashContent;
            this.weightGrams = weightGrams;
            this.textureName = textureName;
            this.description = description;
        }

        public String getName() { return name; }
        public double getProteinContent() { return proteinContent; }
        public double getAshContent() { return ashContent; }
        public int getWeightGrams() { return weightGrams; }
        public String getTextureName() { return textureName; }
        public String getDescription() { return description; }
    }
}