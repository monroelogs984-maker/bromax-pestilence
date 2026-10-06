package com.bromax.pestilence.apothecary;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** What is on the Apothecary Table: a base (bottle, bowl, paper) and up to four ingredients. */
public record ApothecaryInput(ItemStack base, List<ItemStack> ingredients) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? base : ingredients.get(index - 1);
    }

    @Override
    public int size() {
        return 1 + ingredients.size();
    }
}
