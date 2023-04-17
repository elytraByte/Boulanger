package org.l3e.Boulanger.recipe;

import com.google.gson.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.l3e.Boulanger.Boulanger;


public class SeparatorRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private static ItemStack primaryOutput;
    private static ItemStack secondaryOutput;
    private static ItemStack tertiaryOutput;
    private static ItemStack fourthOutput;
    private final NonNullList<Ingredient> recipeItems;
    private static float chance;



    public SeparatorRecipe(ResourceLocation id, ItemStack primaryOutput, ItemStack secondaryOutput,
                           ItemStack tertiaryOutput, ItemStack fourthOutput, NonNullList<Ingredient> recipeItems, float chance) {
        this.id = id;
        this.primaryOutput = primaryOutput;
        this.secondaryOutput = secondaryOutput;
        this.tertiaryOutput = tertiaryOutput;
        this.fourthOutput = fourthOutput;
        this.recipeItems = recipeItems;
        this.chance = chance;
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
        return primaryOutput;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    public static float chance() {
        return chance;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return primaryOutput;
    }


    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SeparatorRecipe.Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return SeparatorRecipe.Type.INSTANCE;
    }

    /*public ItemStack getResultItem() {
        return output.copy();
    }*/

    public static class Type implements RecipeType<SeparatorRecipe> {
        private Type() { }
        public static final SeparatorRecipe.Type INSTANCE = new SeparatorRecipe.Type();
        public static final String ID = "separating";
    }


    public static class Serializer implements RecipeSerializer<SeparatorRecipe> {
        public static final SeparatorRecipe.Serializer INSTANCE = new SeparatorRecipe.Serializer();
        public static final ResourceLocation ID =
                new ResourceLocation(Boulanger.MOD_ID, "separating");


        public float getChance(JsonObject obj) {

            JsonElement outputs = obj.getAsJsonArray("output").get(0);

            return  outputs.getAsJsonObject().get("chance").getAsFloat();


        }

        public ItemStack getOutputs(JsonObject json) {

            JsonElement element = json.getAsJsonArray("output").get(0);
            System.out.println(element);

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(element.getAsJsonObject().get("item").getAsString()));
            System.out.println(item);

            return new ItemStack(item, 1);

        }

        public ItemStack getSecondaryOutput(JsonObject json) {
            JsonElement element = json.getAsJsonArray("output").get(1);
            System.out.println(element);

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(element.getAsJsonObject().get("item").getAsString()));
            System.out.println(item);

            return new ItemStack(item, 1);
        }

        public static ItemStack getSecondaryResult() {
            return secondaryOutput;
        }

        public ItemStack getTertiaryOutput(JsonObject json) {
            JsonElement element = json.getAsJsonArray("output").get(2);
            System.out.println(element);

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(element.getAsJsonObject().get("item").getAsString()));
            System.out.println(item);

            return new ItemStack(item, 1);
        }

        public static ItemStack getTertiaryResult() {
            return tertiaryOutput;
        }

        public ItemStack getFourthOutput(JsonObject json) {
            JsonElement element = json.getAsJsonArray("output").get(3);
            System.out.println(element);

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(element.getAsJsonObject().get("item").getAsString()));
            System.out.println(item);

            return new ItemStack(item, 1);
        }

        public static ItemStack getFourthResult() {
            return fourthOutput;
        }




        @Override
        public SeparatorRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {

            float chance = getChance(pSerializedRecipe);
            ItemStack output0 = getOutputs(pSerializedRecipe);
            ItemStack output1 = getSecondaryOutput(pSerializedRecipe);
            ItemStack output2 = getTertiaryOutput(pSerializedRecipe);
            ItemStack output3 = getFourthOutput(pSerializedRecipe);

            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            return new SeparatorRecipe(pRecipeId, output0, output1, output2, output3, inputs, chance);

        }

        @Override
        public @Nullable SeparatorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buf.readInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(buf));
            }

            ItemStack output = buf.readItem();
            ItemStack output1 = buf.readItem();
            ItemStack output2 = buf.readItem();
            ItemStack output3 = buf.readItem();
            float chance = buf.readFloat();
            return new SeparatorRecipe(id, output, output1, output2, output3, inputs, chance);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SeparatorRecipe recipe) {
            buf.writeInt(recipe.getIngredients().size());


            for (Ingredient ing : recipe.getIngredients()) {
                ing.toNetwork(buf);
            }
            buf.writeItem(recipe.primaryOutput);
            buf.writeItem(recipe.secondaryOutput);
            buf.writeItem(recipe.tertiaryOutput);
            buf.writeItem(recipe.fourthOutput);
            buf.writeFloat(chance);
        }
    }
}