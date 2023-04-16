package org.l3e.Boulanger.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.l3e.Boulanger.Boulanger;

public class ThreshingMachineRecipe  implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    //private static ItemStack extra;



    public ThreshingMachineRecipe(ResourceLocation id, ItemStack output,
                                  NonNullList<Ingredient> recipeItems) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        //this.extra = extra;
    }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if(pLevel.isClientSide()) {
            return false;
        }

        return recipeItems.get(0).test(pContainer.getItem(1));
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
        return ThreshingMachineRecipe.Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return ThreshingMachineRecipe.Type.INSTANCE;
    }

    public static class Type implements RecipeType<ThreshingMachineRecipe> {
        private Type() { }
        public static final ThreshingMachineRecipe.Type INSTANCE = new ThreshingMachineRecipe.Type();
        public static final String ID = "threshing";
    }


    public static class Serializer implements RecipeSerializer<ThreshingMachineRecipe> {
        public static final ThreshingMachineRecipe.Serializer INSTANCE = new ThreshingMachineRecipe.Serializer();
        public static final ResourceLocation ID =
                new ResourceLocation(Boulanger.MOD_ID, "threshing");

        @Override
        public ThreshingMachineRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));
            //ItemStack extra = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "extra"));
            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            return new ThreshingMachineRecipe(pRecipeId, output, inputs);
        }

        @Override
        public @Nullable ThreshingMachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buf.readInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(buf));
            }

            ItemStack output = buf.readItem();
            //ItemStack extra = buf.readItem();
            return new ThreshingMachineRecipe(id, output, inputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ThreshingMachineRecipe recipe) {
            buf.writeInt(recipe.getIngredients().size());


            for (Ingredient ing : recipe.getIngredients()) {
                ing.toNetwork(buf);
            }
            buf.writeItem(recipe.output);
            //buf.writeItem(recipe.extra);
        }
    }
}