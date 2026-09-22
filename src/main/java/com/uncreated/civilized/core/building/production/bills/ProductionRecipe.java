package com.uncreated.civilized.core.building.production.bills;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * A recipe found for the input items of a production bill.
 *
 * @param input
 *           the input items, as the recipe saw them
 * @param result
 *           what the recipe makes from those items
 */
public record ProductionRecipe(RecipeHolder<?> recipe, RecipeInput input, ItemStack result) {
}
