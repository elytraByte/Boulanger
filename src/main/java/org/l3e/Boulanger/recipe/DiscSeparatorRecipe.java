package org.l3e.Boulanger.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.l3e.Boulanger.Boulanger;

public class DiscSeparatorRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;



    public DiscSeparatorRecipe(ResourceLocation id, ItemStack output,
                                  NonNullList<Ingredient> recipeItems) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
    }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if(pLevel.isClientSide()) {
            return false;
        }

        return recipeItems.get(0).test(pContainer.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer p_44001_, RegistryAccess p_267165_) {
        return output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return output;
    }


    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DiscSeparatorRecipe.Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return DiscSeparatorRecipe.Type.INSTANCE;
    }

    public static ItemStack getOutputs(JsonObject json) {

        JsonElement element = json.getAsJsonArray("output").get(0);
//            System.out.println(element);

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(element.getAsJsonObject().get("item").getAsString()));
//            System.out.println(item);

        return new ItemStack(item, 1);

    }

    public static class Type implements RecipeType<DiscSeparatorRecipe> {
        private Type() { }
        public static final DiscSeparatorRecipe.Type INSTANCE = new DiscSeparatorRecipe.Type();
        public static final String ID = "disc_separating";
    }


    public static class Serializer implements RecipeSerializer<DiscSeparatorRecipe> {
        public static final DiscSeparatorRecipe.Serializer INSTANCE = new DiscSeparatorRecipe.Serializer();
        public static final ResourceLocation ID =
                new ResourceLocation(Boulanger.MOD_ID, "disc_separating");

        @Override
        public DiscSeparatorRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            ItemStack output = getOutputs(pSerializedRecipe);
            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            return new DiscSeparatorRecipe(pRecipeId, output, inputs);
        }

        @Override
        public @Nullable DiscSeparatorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buf.readInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(buf));
            }

            ItemStack output = buf.readItem();
            return new DiscSeparatorRecipe(id, output, inputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DiscSeparatorRecipe recipe) {
            buf.writeInt(recipe.getIngredients().size());


            for (Ingredient ing : recipe.getIngredients()) {
                ing.toNetwork(buf);
            }
            buf.writeItem(recipe.output);
        }
    }
}
