package com.uncreated.civilized.core.building.production.orders;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.BuildingStockRequirement;
import com.uncreated.civilized.core.building.production.PendingProductionOutput;
import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.orders.recipe.AssembledRecipe;
import com.uncreated.civilized.core.building.production.orders.recipe.RecipeAssembler;

import lombok.Getter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

@Getter
public abstract class ProductionOrder {

   private final ProductionBill bill;
   private final String key;
   protected final RecipeAssembler<?, ?> recipeAssembler;
   private final Level level;

   public ProductionOrder(String key, ProductionBill bill, ServerLevel level) {
      this.bill = bill;
      this.key = key;

      Optional<RecipeHolder<?>> recipeHolder = bill.resolveRecipe(level.getServer().getRecipeManager());
      if (recipeHolder.isEmpty())
         throw new UnsupportedOperationException("Could not create production order - recipe not found");

      recipeAssembler = getRecipeAssembler(recipeHolder.get().value(), level.registryAccess());
      // recipeAssembler = switch (bill.getProductionType()) {
      // case CRAFTING -> new CraftingRecipeAssembler(recipeHolder.get().value(), level.registryAccess());
      // case SMELTING, BLASTING, SMOKING ->
      // new SingleItemRecipeAssembler(recipeHolder.get().value(), level.registryAccess());
      // };

      this.level = level;
   }

   protected abstract RecipeAssembler<?, ?> getRecipeAssembler(Recipe<?> value, RegistryAccess registryAccess);

   public List<BuildingStockRequirement> createStandardIngredientRequirements(int recipeSlot, int batchSize) {

      List<BuildingStockRequirement> requirements = new LinkedList<>();

      List<Ingredient> ingredients = recipeAssembler.getRecipe().placementInfo().ingredients();

      ItemFilter itemFilter = this.bill.getItemFilters().getFilter(recipeSlot);

      for (Ingredient ingredient : ingredients) {

         BuildingStockRequirement requirement =
               new BuildingStockRequirement(String.format(
                       "%s:%s",
                       bill.getProductionType(),
                       bill.getMinecraftRecipeName()
               ),
                     i -> isAcceptableIngredient(i, ingredient, itemFilter),
                     1,
                     batchSize);
         // makes it so that duplicate ingredients are still taken
         requirement.disregardExistingCarriedStock(true);
         requirements.add(requirement);
      }

      return requirements;
   }

   public int calculateFinalProductDeficit(List<Container> stockChests) {
      // compute the current deficit of final product items so we know how many ingredients to import
      int stockDeficit = 0;
      int productionBatchSize = bill.getProductionStrategy().productionBatchSize();

      Optional<ItemStack> defaultResultItem = recipeAssembler.getDefaultResultItem();
      if (defaultResultItem.isPresent()) {
         stockDeficit =
               bill.getProductionStrategy().calculateStockDeficit(calculateStock(stockChests, defaultResultItem.get()));

         if (stockDeficit > productionBatchSize)
            stockDeficit = productionBatchSize;
      }
      return stockDeficit;
   }

   public static boolean isAcceptableIngredient(ItemStack itemStack, Ingredient ingredient, ItemFilter filter) {

      boolean validIngredient = ingredient.acceptsItem(itemStack.getItemHolder());
      if (!validIngredient)
         return false;

      return filter.acceptsItem(itemStack);
   }

   public PendingProductionOutput getNextOutput(List<Container> ingredientsChests, List<Container> stockChests) {

      // ingredients filter is always slot 0
      ItemFilter ingredientsFilter = getBill().getItemFilters().getFilter(0);

      RecipeAssembler.RecipeSatisfiedResult recipeSatisfied =
            recipeAssembler.isRecipeSatisfied(ingredientsChests, ingredientsFilter);

      AssembledRecipe<?> assembledRecipe = recipeAssembler.assembleRecipe(recipeSatisfied.availableInput());

      int stockDeficit =
            bill.getProductionStrategy()
                  .calculateStockDeficit(calculateStock(stockChests, assembledRecipe.resultItem()));

      return new PendingProductionOutput(
            recipeAssembler.getRecipe(),
            assembledRecipe,
            recipeSatisfied.satisfied(),
            stockDeficit,
            ingredientsChests);
   }

   protected AggregateItemStack calculateStock(Collection<Container> containers, ItemStack resultItem) {
      AggregateItemStack stock = new AggregateItemStack();
      containers.forEach(c -> {
         for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack itemStack = c.getItem(i);
            if (itemStack.isEmpty())
               continue;

            if (!itemStack.is(resultItem.getItem()))
               continue;

            stock.add(itemStack);
         }
      });

      return stock;
   }
}
