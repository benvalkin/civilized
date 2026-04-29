package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.PendingShipment;
import com.uncreated.civilized.core.building.logistics.orders.LogisticsOrder;
import com.uncreated.civilized.core.building.logistics.orders.LogisticsOrders;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportOrder;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;

public class FetchImportsFromStorehouse extends ExchangeResourcesAtBuilding {

   public FetchImportsFromStorehouse() {
      super(WorkStates.FETCHING_IMPORTS_FROM_STOREHOUSE, 120 * 20, 30 * 20);
   }

   @Override
   protected Optional<Building> findTargetBuilding(ServerLevel level, CivilizedVillager villager) {
      return ServerBuildingsStore.INSTANCE.findStorehouse(villager.getInfo().getSettlementId())
            .flatMap(LoadedBuildings::checkLoaded)
            .map(LoadedBuilding::getBuilding);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      return areThereItemsToImport(getSettlement());
   }

   @Override
   protected void exchangeResources(ServerLevel level, CivilizedVillager villager, long tickTime) {

      LogisticsOrders<ImportOrder> importOrders =
            getSettlement().getBehaviour().getLogisticsManager().getImportOrders(getHome().getBuilding());

      List<Container> source = LogisticsOrder.findChests(level, targetbuilding);
      List<Container> destination = LogisticsOrder.findChests(level, getHome().getBuilding());
      boolean importsPossible = false;
      for (ImportOrder order : importOrders.orders()) {

         PendingShipment shipment = order.getNextShipment(source, destination);
         if (!shipment.shouldShip())
            continue;

         if (order.takeShipment(villager, shipment))
            importsPossible = true;
      }

      if (importsPossible)
         getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_IMPORTS_AT_HOME);
   }

   private boolean areThereItemsToImport(LoadedSettlement settlement) {

      List<Container> source = LogisticsOrder.findChests(settlement.getLevel(), targetbuilding);
      List<Container> destination = LogisticsOrder.findChests(settlement.getLevel(), getHome().getBuilding());

      LogisticsManager logisticsManager = settlement.getBehaviour().getLogisticsManager();
      LogisticsOrders<ImportOrder> importOrders = logisticsManager.getImportOrders(getHome().getBuilding());

      for (ImportOrder order : importOrders.orders()) {

         PendingShipment shipment = order.getNextShipment(source, destination);
         if (shipment.shouldShip()) {
            return true;
         }
      }

      return false;
   }
}
