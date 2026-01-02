package net.boulangermod.boulanger.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.boulangermod.boulanger.util.StreamCodecsCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class IngredientRequirement {
    private final ResourceLocation itemId;
    private final double amount;

    // cached resolved item (optional)
    private @Nullable Item cachedItem;

    public IngredientRequirement(ResourceLocation itemId, double amount) {
        this.itemId = itemId;
        this.amount = amount;
        if (itemId == null) throw new IllegalArgumentException("itemId cannot be null");
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0 for " + itemId);
    }

    public ResourceLocation getItemId() { return itemId; }
    public double getAmount() { return amount; }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        Item required = cachedItem;
        if (required == null) {
            required = BuiltInRegistries.ITEM.getOptional(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + itemId));
            cachedItem = required;
        }
        return stack.getItem() == required;
    }

    // ---- codecs ----
    public static final MapCodec<IngredientRequirement> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(IngredientRequirement::getItemId),
            Codec.DOUBLE.fieldOf("amount").forGetter(IngredientRequirement::getAmount)
    ).apply(inst, IngredientRequirement::new));

    public static final Codec<IngredientRequirement> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientRequirement> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodecsCompat.RESOURCE_LOCATION, IngredientRequirement::getItemId,
                    StreamCodecsCompat.DOUBLE, IngredientRequirement::getAmount,
                    IngredientRequirement::new
            );
}
