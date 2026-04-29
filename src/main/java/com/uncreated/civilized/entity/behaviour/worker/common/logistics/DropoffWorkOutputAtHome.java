package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;

import net.minecraft.server.level.ServerLevel;

public class DropoffWorkOutputAtHome extends ExchangeResourcesAtBuilding {

   public DropoffWorkOutputAtHome() {
      super(WorkStates.DROPPING_OFF_WORK_OUTPUT_AT_HOME, 120 * 20, 30 * 20);
   }

   @Override
   protected Optional<Building> findTargetBuilding(ServerLevel level, CivilizedVillager villager) {
      return LoadedBuildings.checkLoaded(villager.getInfo().getHomeBuildingId()).map(LoadedBuilding::getBuilding);
   }

   @Override
   protected void exchangeResources(ServerLevel level, CivilizedVillager villager, long tickTime) {
      dumpInventoryToChests(villager.getWorkInputInventory());
      dumpInventoryToChests(villager.getWorkOutputInventory());
   }
}
