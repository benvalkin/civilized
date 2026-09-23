package com.uncreated.civilized.core.building.production;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.RecipeSlotType;
import com.uncreated.civilized.core.building.production.orders.ProductionOrder;
import com.uncreated.civilized.core.building.production.orders.recipe.AssembledRecipe;

import lombok.EqualsAndHashCode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public record PendingProductionOutput(Recipe<?> recipe, AssembledRecipe<?> assembledRecipe, boolean canProduce,
      int stockDeficit, Collection<Container> sourceContainers) {

   public List<ConsumableIngredientStack> getConsumableIngredients() {

      List<ConsumableIngredientStack> consumableIngredients = new ArrayList<>();

      for (ItemStack ingredient : assembledRecipe.availableIngredients()) {

         ConsumableIngredientStack consumableIngredientStack = new ConsumableIngredientStack();

         final int quota = ingredient.getCount();
         int ingredientsFound = 0;
         for (Container sourceContainer : sourceContainers) {

            for (int i = 0; i < sourceContainer.getContainerSize(); i++) {
               ItemStack item = sourceContainer.getItem(i);

               if (!item.is(ingredient.getItem()))
                  continue;

               int stillMissing = quota - ingredientsFound;
               int availableSubStackCount = Math.min(stillMissing, item.getCount());

               consumableIngredientStack
                     .add(new SourceIngredientSubStack(item.copyWithCount(availableSubStackCount), sourceContainer, i));

               ingredientsFound += availableSubStackCount;

               if (ingredientsFound == quota)
                  break;
            }

            if (ingredientsFound == quota)
               break;
         }

         consumableIngredients.add(consumableIngredientStack);
      }

      return consumableIngredients;
   }

   public ConsumableIngredientStack getConsumableFuel(
         int recipeSlot,
         ProductionOrder productionOrder,
         int quota,
         ServerLevel level) {

      RecipeType<?> recipeType = productionOrder.getBill().getProductionType().recipeType();
      RecipeSlotType fuelSlot = productionOrder.getBill().getProductionType().recipeSlots().get(recipeSlot);

      ItemFilter itemFilter = productionOrder.getBill().getItemFilters().getFilter(recipeSlot);

      ConsumableIngredientStack consumableIngredientStack = new ConsumableIngredientStack();

      Item requiredFuelItemType = Items.AIR;
      int longestBurnTimeEncountered = 0;

      int ingredientsFound = 0;
      for (Container sourceContainer : sourceContainers) {

         for (int i = 0; i < sourceContainer.getContainerSize(); i++) {
            ItemStack item = sourceContainer.getItem(i);

            if (!fuelSlot.isItemAllowed(item, level))
               continue;

            if (!itemFilter.acceptsItem(item))
               continue;

            // we are calling getBurnTime twice here because of fuel.isItemAllowed - probably not the best thing
            int burnTime = item.getBurnTime(recipeType, level.fuelValues());

            if (burnTime > longestBurnTimeEncountered) {
               // new fuel item found that is better than the current type.
               longestBurnTimeEncountered = burnTime;
               requiredFuelItemType = item.getItem();
               consumableIngredientStack = new ConsumableIngredientStack();
            }

            if (!item.is(requiredFuelItemType))
               continue;

            int stillMissing = quota - ingredientsFound;
            int availableSubStackCount = Math.min(stillMissing, item.getCount());

            consumableIngredientStack
                  .add(new SourceIngredientSubStack(item.copyWithCount(availableSubStackCount), sourceContainer, i));

            ingredientsFound += availableSubStackCount;

            if (ingredientsFound == quota)
               break;
         }

         if (ingredientsFound == quota)
            break;
      }

      return consumableIngredientStack;
   }

   @EqualsAndHashCode
   public static final class ConsumableIngredientStack {
      private ItemStack aggregateStack;
      private final List<SourceIngredientSubStack> subStacks;

      public ConsumableIngredientStack() {
         this.aggregateStack = ItemStack.EMPTY;
         this.subStacks = new ArrayList<>();
      }

      public void add(SourceIngredientSubStack subStack) {
         if (aggregateStack.isEmpty())
            aggregateStack = subStack.itemStack().copy();
         else
            aggregateStack.shrink(subStack.itemStack().getCount());

         subStacks.add(subStack);
      }

      public ItemStack aggregateStack() {
         return aggregateStack;
      }

      public List<SourceIngredientSubStack> subStacks() {
         return subStacks;
      }
   }

   public record SourceIngredientSubStack(ItemStack itemStack, Container sourceContainer, int sourceContainerSlot) {

   }

   public void consumeIngredients(List<ConsumableIngredientStack> sourceIngredientStacks) {

      for (ConsumableIngredientStack sourceIngredientStack : sourceIngredientStacks) {

         for (SourceIngredientSubStack subStack : sourceIngredientStack.subStacks) {
            ItemStack ingredientSubStack = subStack.itemStack();
            ItemStack itemInContainer = subStack.sourceContainer().getItem(subStack.sourceContainerSlot());

            if (!itemInContainer.is(ingredientSubStack.getItem()))
               continue;

            itemInContainer.shrink(ingredientSubStack.getCount());
            subStack.sourceContainer().setItem(subStack.sourceContainerSlot(), itemInContainer);

         }
      }
   }
}
