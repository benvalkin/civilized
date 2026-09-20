package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TransferToBuildingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.BuildingStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.core.building.logistics.orders.LogisticsOrder;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

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

   private final BuildingStockRequirement everything =
         new BuildingStockRequirement(
               "export_home_goods_to_storehouse",
               i -> !i.isEmpty(),
               1,
               ItemStockRequirement.UNLIMITED);

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {

      getBehaviourCooldowns().startCooldown(Cooldowns.START, Duration.of(15, ChronoUnit.SECONDS), level.getGameTime());

      Optional<LoadedBuilding> storehouse = findStorehouse(getSettlement());
      if (storehouse.isEmpty())
         return;

      this.storehouse = storehouse.get();

      List<Container> homeChests = LogisticsOrder.findChests(level, getHome().getBuilding());
      List<Container> storehouseChests = LogisticsOrder.findChests(level, this.storehouse.getBuilding());
      List<LoadedBuilding> home = List.of(getHome());
      if (!getSharedCooldowns().hasCooldown(Cooldowns.EXPORT_RUN, level.getGameTime()) && checkForExportOrders()) {

         Optional<TransferToBuildingInstruction> transferToBuildingInstruction =
               TransferToBuildingInstruction.tryCreate(everything, this.storehouse, home);
         if (transferToBuildingInstruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), transferToBuildingInstruction);
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
         }
      } else if (!getSharedCooldowns().hasCooldown(Cooldowns.IMPORT_RUN, level.getGameTime())
            && checkForImportOrders(storehouseChests, homeChests)) {
         getStateMachine().queueActionOnce(WorkStates.FETCHING_IMPORTS_FROM_STOREHOUSE);
      }
   }

   private boolean checkForExportOrders() {
      return getHome().findChests().stream().anyMatch(c -> !c.isEmpty());
   }

   private boolean checkForImportOrders(List<Container> source, List<Container> destination) {

      return false;
      // LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();
      // LogisticsOrders<ImportOrder> importOrders = logisticsManager.getImportOrders(getHome().getBuilding());
      //
      // for (ImportOrder order : importOrders.orders()) {
      //
      // PendingShipment shipment = order.getNextShipment(source, destination);
      // if (shipment.shouldShip()) {
      // getStateMachine().queueActionOnce(WorkStates.FETCHING_IMPORTS_FROM_STOREHOUSE);
      // return true;
      // }
      // }
      //
      // return false;
   }
}
