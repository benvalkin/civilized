package com.uncreated.civilized.core.building.production;

import com.uncreated.civilized.core.building.production.orders.ProductionOrder;

public record PendingProduction(ProductionOrder order, PendingProductionOutput output) {

}
