package com.uncreated.civilized.core.building.production.orders.recipe;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

public abstract class RecipeAssembler<TRecipe extends Recipe<?>, TRecipeInput extends RecipeInput> {

   private final HolderLookup.Provider registryAccess;

   public RecipeAssembler(HolderLookup.Provider registryAccess) {
      this.registryAccess = registryAccess;
   }

   public abstract TRecipe getRecipe();

   public abstract AssembledRecipe<TRecipeInput> assembleRecipe(List<ItemStack> availableInput);

   public abstract Optional<ItemStack> getDefaultResultItem();

   public abstract RecipeSatisfiedResult isRecipeSatisfied(List<Container> containers, ItemFilter ingredientsFilter);

   public record RecipeSatisfiedResult(boolean satisfied, int numberOfIngredientsSatisfied, int numberOfRequiredIngredients,
                                       List<ItemStack> availableInput) {

   }
}
