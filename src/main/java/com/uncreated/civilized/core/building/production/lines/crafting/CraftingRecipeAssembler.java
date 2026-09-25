package com.uncreated.civilized.core.building.production.lines.crafting;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.ItemFilters;
import com.uncreated.civilized.core.building.production.orders.recipe.AssembledRecipe;
import com.uncreated.civilized.core.building.production.orders.recipe.RecipeAssembler;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public class CraftingRecipeAssembler extends RecipeAssembler<CraftingRecipe, CraftingInput> {

    @Getter
    private final CraftingRecipe recipe;
    private final HolderLookup.Provider registryAccess;

    public CraftingRecipeAssembler(Recipe<?> recipe, HolderLookup.Provider registryAccess) {
        super(registryAccess);
        if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
            throw new UnsupportedOperationException("Unsupported recipe type: " + recipe);
        }

        this.recipe = craftingRecipe;
        this.registryAccess = registryAccess;
    }

    public RecipeSatisfiedResult isRecipeSatisfied(List<Container> containers, ItemFilter ingredientsFilter) {

        // 3x3 "crafting window" in array form
        ItemStack[] craftingWindow = new ItemStack[9];
        Arrays.fill(craftingWindow, ItemStack.EMPTY);

        IntList slotsToIngredientIndex = getRecipe().placementInfo().slotsToIngredientIndex();
        List<Ingredient> ingredients = getRecipe().placementInfo().ingredients();

        int ingredientsSatisfied = 0;

        for (int c = 0; c < containers.size(); c++) {
            Container container = containers.get(c);
            for (int s = 0; s < container.getContainerSize(); s++) {

                ItemStack itemStack = container.getItem(s);
                if (itemStack.isEmpty())
                    continue;

                if (!ingredientsFilter.acceptsItem(itemStack))
                    continue;

                // copy the candidate ingredient's item because we will decrement it later as we "add it" to the
                // "crafting window"
                ItemStack candidateIngredient = container.getItem(s).copy();

                // check each ingredient to see if the candidate matches, at the same time building the final crafting
                // window

                for (int i = 0; i < slotsToIngredientIndex.size(); i++) {

                    int ingredientIndex = slotsToIngredientIndex.getInt(i);
                    if (ingredientIndex == -1) // crafting window slots remain empty have a -1 ingredient index
                        continue;

                    if (!craftingWindow[i].isEmpty())
                        continue; // ignore this ingredient as we've already using it

                    Ingredient ingredient = ingredients.get(ingredientIndex);

                    if (ingredient.acceptsItem(candidateIngredient.getItemHolder())) {
                        // if this item is a valid ingredient for this slot, move it to the "crafting window"
                        craftingWindow[i] = candidateIngredient.copyWithCount(1);
                        ingredientsSatisfied++;
                        // mark that we have successfully moved 1 of this item into the "crafting window"
                        candidateIngredient.shrink(1);

                        // if we have satisfied all the required ingredients, we have successfully satisfied this recipe :)
                        if (ingredientsSatisfied == ingredients.size())
                            return new RecipeSatisfiedResult(
                                    true,
                                    ingredientsSatisfied,
                                    ingredients.size(),
                                    Arrays.stream(craftingWindow).toList());

                        if (candidateIngredient.isEmpty())
                            // stop if we've used up this candidate ingredient, and move on to the next one
                            break;
                    }
                }

            }
        }

        // otherwise, we don't have the correct ingredients to satisfy the recipe
        return new RecipeSatisfiedResult(
                false,
                ingredientsSatisfied,
                ingredients.size(),
                Arrays.stream(craftingWindow).toList());
    }

    public AssembledRecipe<CraftingInput> assembleRecipe(List<ItemStack> availableIngredients) {
        CraftingInput input = CraftingInput.of(3, 3, availableIngredients);
        ItemStack result = recipe.assemble(input, registryAccess);
        return new AssembledRecipe<>(availableIngredients, input, result);
    }

    public Optional<ItemStack> getDefaultResultItem() {
       // the only way to get the output itemStack of a recipe is by passing in a dummy recipe input.
       // for crafting and single item recipes, this input list can be empty - however, this may not work for other
       // recipes
       CraftingInput dummyCraftingInput = CraftingInput.Positioned.EMPTY.input();
       ItemStack resultItem = recipe.assemble(dummyCraftingInput, registryAccess);
       if (resultItem.isEmpty())
          return Optional.empty();

       return Optional.of(resultItem);
    }
}
