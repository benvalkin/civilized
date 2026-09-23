package com.uncreated.civilized.core.building.production;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.strategy.ProductionStrategyType;
import com.uncreated.civilized.core.building.production.orders.ProductionOrder;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;

public abstract class RecipeProductionMachine<Order extends ProductionOrder> {

   private final List<Order> orders;

   public RecipeProductionMachine() {
      orders = new LinkedList<>();
      currentOrderIndex = 0;
      productionTokens = 0;
   }

   public void registerOrder(Order productionOrder) {
      orders.add(productionOrder);
   }

   public void clearOrders() {
      orders.clear();
   }

   public Collection<Order> getOrders() {
      return orders;
   }

   public abstract Order createOrderFromBill(String key, ProductionBill bill, ServerLevel serverLevel);

   public abstract ProductionType getProductionType();

   @Getter
   private int productionTokens;

   private int currentOrderIndex;

   public void consumeToken() {
      productionTokens--;

      if (productionTokens <= 0)
         productionTokens = 0;
   }

   public void consumeTokens(int numberOfTokens) {
      productionTokens -= numberOfTokens;

      if (productionTokens <= 0)
         productionTokens = 0;
   }

   public Optional<Pair<ProductionOrder, PendingProductionOutput>> tryGetNextOrder(
         List<Container> ingredientsChests,
         List<Container> stockChests) {

      if (orders.isEmpty())
         return Optional.empty();

      int attempts = 0;
      do {

         if (currentOrderIndex >= orders.size() && !orders.isEmpty())
            // In case someone removed a product bill and the index is now too high, set it to the last order.
            currentOrderIndex = orders.size() - 1;

         Order currentlyProcessing = orders.get(currentOrderIndex);

         if (productionTokens > 0) {
            Optional<PendingProductionOutput> possibleOutput =
                  checkProductionPossible(currentlyProcessing, ingredientsChests, stockChests);
            if (possibleOutput.isPresent())
               return Optional.of(Pair.of(currentlyProcessing, possibleOutput.get()));
         }

         // if it is not possible to produce the current order, either because it has no ingredients or no more tokens,
         // try move onto the next one
         currentOrderIndex++;

         // reset the cycle if its reaches the end
         if (currentOrderIndex >= orders.size())
            currentOrderIndex = 0;

         // add more tokens
         productionTokens = currentlyProcessing.getBill().getStartingProductionTokens();
         attempts++;
      } while (attempts < orders.size());

      return Optional.empty();
   }

   private Optional<PendingProductionOutput> checkProductionPossible(
         ProductionOrder order,
         List<Container> ingredientsChests,
         List<Container> stockChests) {

      if (!order.getBill().isEnabled())
         return Optional.empty();

      PendingProductionOutput pendingOutput = order.getNextOutput(ingredientsChests, stockChests);
      if (!pendingOutput.canProduce())
         return Optional.empty();

      if (!(order.getBill().getProductionStrategy().getType() == ProductionStrategyType.PRODUCE_INFINITE
            || pendingOutput.stockDeficit() > 0))
         return Optional.empty();

      return Optional.of(pendingOutput);
   }
}
