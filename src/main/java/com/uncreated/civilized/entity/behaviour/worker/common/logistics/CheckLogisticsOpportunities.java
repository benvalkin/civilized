package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.PendingShipment;
import com.uncreated.civilized.core.building.logistics.orders.LogisticsOrder;
import com.uncreated.civilized.core.building.logistics.orders.LogisticsOrders;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportOrder;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;

public class CheckLogisticsOpportunities extends WorkTaskBehaviour {

   private LoadedBuilding storehouse;

   public CheckLogisticsOpportunities() {
      super(WorkStates.CHECK_LOGISTICS_OPPORTUNITIES, true, true, 0, 5 * 20);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      return false; // stop immediately (once-off behaviour)
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      return !getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime());
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {

      getBehaviourCooldowns().startCooldown(Cooldowns.START, Duration.of(15, ChronoUnit.SECONDS), level.getGameTime());

      Optional<LoadedBuilding> storehouse = findStorehouse(getSettlement());
      if (storehouse.isEmpty())
         return;

      this.storehouse = storehouse.get();

      List<Container> homeChests = LogisticsOrder.findChests(level, getHome().getBuilding());
      List<Container> storehouseChests = LogisticsOrder.findChests(level, this.storehouse.getBuilding());

      if (!getSharedCooldowns().hasCooldown(Cooldowns.EXPORT_RUN, level.getGameTime()) && checkForExportOrders()) {
         getStateMachine().queueActionOnce(WorkStates.FETCHING_EXPORTS_FROM_HOME);
      } else if (!getSharedCooldowns().hasCooldown(Cooldowns.IMPORT_RUN, level.getGameTime())
            && checkForImportOrders(storehouseChests, homeChests)) {
         getStateMachine().queueActionOnce(WorkStates.FETCHING_IMPORTS_FROM_STOREHOUSE);
      }
   }

   private boolean checkForExportOrders() {
      return FetchExportsFromHome
            .areThereItemsToExport(getSettlement(), getHome().getBuilding(), storehouse.getBuilding());
   }

   private boolean checkForImportOrders(List<Container> source, List<Container> destination) {

      LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();
      LogisticsOrders<ImportOrder> importOrders = logisticsManager.getImportOrders(getHome().getBuilding());

      for (ImportOrder order : importOrders.orders()) {

         PendingShipment shipment = order.getNextShipment(source, destination);
         if (shipment.shouldShip()) {
            getStateMachine().queueActionOnce(WorkStates.FETCHING_IMPORTS_FROM_STOREHOUSE);
            return true;
         }
      }

      return false;
   }
}
