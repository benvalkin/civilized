package com.uncreated.civilized.core.building.production.bills.strategy;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class ProduceUpTo implements IProductionStrategy {

   private final int max;
   private final int productionBatchSize;

   public ProduceUpTo(int max, int productionBatchSize) {
      this.max = max;
      this.productionBatchSize = productionBatchSize;
   }

   @Override
   public int calculateStockDeficit(AggregateItemStack stockChestsStock) {
      int deficit = max - stockChestsStock.getCount();
      return Math.max(deficit, 0);

   }

   @Override
   public ProductionStrategyType getType() {
      return ProductionStrategyType.PRODUCE_UP_TO;
   }
}
