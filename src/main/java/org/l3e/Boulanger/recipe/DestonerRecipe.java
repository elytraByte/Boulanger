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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.l3e.Boulanger.Boulanger;

public class DestonerRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private static ItemStack primaryOutput;
    private static ItemStack secondaryOutput;
    private static ItemStack tertiaryOutput;
    private final NonNullList<Ingredient> recipeItems;
    private static float[] chance;



    public DestonerRecipe(ResourceLocation id, ItemStack primaryOutput, ItemStack secondaryOutput,
                           ItemStack tertiaryOutput, NonNullList<Ingredient> recipeItems, float[] chance) {
        this.id = id;
        this.primaryOutput = primaryOutput;
        this.secondaryOutput = secondaryOutput;
        this.tertiaryOutput = tertiaryOutput;
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

    public static float[] chance() {
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
        return DestonerRecipe.Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return DestonerRecipe.Type.INSTANCE;
    }

    public static class Type implements RecipeType<DestonerRecipe> {
        private Type() { }
        public static final DestonerRecipe.Type INSTANCE = new DestonerRecipe.Type();
        public static final String ID = "destoning";
    }


    public static class Serializer implements RecipeSerializer<DestonerRecipe> {
        public static final DestonerRecipe.Serializer INSTANCE = new DestonerRecipe.Serializer();
        public static final ResourceLocation ID =
                new ResourceLocation(Boulanger.MOD_ID, "destoning");


        public float[] getChances(JsonObject obj) {

            float[] arrayChances = new float[3];

            JsonElement o1 = obj.getAsJsonArray("output").get(0);
            arrayChances[0] = o1.getAsJsonObject().get("chance").getAsFloat();

            JsonElement o2 = obj.getAsJsonArray("output").get(1);
            arrayChances[1] = o2.getAsJsonObject().get("chance").getAsFloat();

            JsonElement o3 = obj.getAsJsonArray("output").get(2);
            arrayChances[2] = o3.getAsJsonObject().get("chance").getAsFloat();

            return arrayChances;
        }

        public ItemStack getOutputs(JsonObject json) {

            JsonElement element = json.getAsJsonArray("output").get(0);
//            System.out.println(element);

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(element.getAsJsonObject().get("item").getAsString()));
//            System.out.println(item);

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
        
        @Override
        public DestonerRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            
            float[] chance = getChances(pSerializedRecipe);
            ItemStack output0 = getOutputs(pSerializedRecipe);
            ItemStack output1 = getSecondaryOutput(pSerializedRecipe);
            ItemStack output2 = getTertiaryOutput(pSerializedRecipe);

            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            return new DestonerRecipe(pRecipeId, output0, output1, output2, inputs, chance);

        }

        @Override
        public @Nullable DestonerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buf.readInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(buf));
            }

            ItemStack output = buf.readItem();
            ItemStack output1 = buf.readItem();
            ItemStack output2 = buf.readItem();
            chance[0] = buf.readFloat();
            chance[1] = buf.readFloat();
            chance[2] = buf.readFloat();

            return new DestonerRecipe(id, output, output1, output2, inputs, chance);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DestonerRecipe recipe) {
            buf.writeInt(recipe.getIngredients().size());


            for (Ingredient ing : recipe.getIngredients()) {
                ing.toNetwork(buf);
            }
            buf.writeItem(recipe.primaryOutput);
            buf.writeItem(recipe.secondaryOutput);
            buf.writeItem(recipe.tertiaryOutput);
            buf.writeFloat(chance[0]);
            buf.writeFloat(chance[1]);
            buf.writeFloat(chance[2]);
        }
    }
}
