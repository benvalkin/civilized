package com.uncreated.civilized.core.building.production.bills.strategy;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class ProduceInfinite implements IProductionStrategy {

   private final int productionBatchSize;

   public ProduceInfinite(int productionBatchSize) {
      this.productionBatchSize = productionBatchSize;
   }

   @Override
   public int calculateStockDeficit(AggregateItemStack stockChestsStock) {
      return Integer.MAX_VALUE;
   }

   @Override
   public ProductionStrategyType getType() {
      return ProductionStrategyType.PRODUCE_INFINITE;
   }
}
