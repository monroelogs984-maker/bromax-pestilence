package com.bromax.pestilence.apothecary;

import com.bromax.pestilence.ModRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-driven Apothecary Table recipe (type bromaxpestilence:apothecary): a base, 1-4 shapeless
 * ingredients, a result, and whether it needs heat (a fire, lit campfire, lava or lit brazier
 * within two blocks of the table).
 */
public record ApothecaryRecipe(Ingredient base, List<Ingredient> ingredients, ItemStack result, boolean heated)
        implements Recipe<ApothecaryInput> {

    public static final MapCodec<ApothecaryRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(ApothecaryRecipe::base),
            Ingredient.CODEC_NONEMPTY.listOf(1, 4).fieldOf("ingredients").forGetter(ApothecaryRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ApothecaryRecipe::result),
            Codec.BOOL.optionalFieldOf("heated", false).forGetter(ApothecaryRecipe::heated)
    ).apply(i, ApothecaryRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApothecaryRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ApothecaryRecipe::base,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), ApothecaryRecipe::ingredients,
            ItemStack.STREAM_CODEC, ApothecaryRecipe::result,
            ByteBufCodecs.BOOL, ApothecaryRecipe::heated,
            ApothecaryRecipe::new);

    @Override
    public boolean matches(ApothecaryInput input, Level level) {
        if (!base.test(input.base()) || input.ingredients().size() != ingredients.size()) return false;
        List<Ingredient> left = new ArrayList<>(ingredients);
        for (ItemStack stack : input.ingredients()) {
            boolean found = false;
            for (int i = 0; i < left.size(); i++) {
                if (left.get(i).test(stack)) {
                    left.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return left.isEmpty();
    }

    @Override
    public ItemStack assemble(ApothecaryInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.APOTHECARY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRegistry.APOTHECARY_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ApothecaryRecipe> {
        @Override
        public MapCodec<ApothecaryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ApothecaryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
