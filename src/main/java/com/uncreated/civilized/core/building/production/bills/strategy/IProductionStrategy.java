package com.uncreated.civilized.core.building.production.bills.strategy;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;

public interface IProductionStrategy {
   int calculateStockDeficit(AggregateItemStack stockChestsStock);

   ProductionStrategyType getType();

   int productionBatchSize();
}
