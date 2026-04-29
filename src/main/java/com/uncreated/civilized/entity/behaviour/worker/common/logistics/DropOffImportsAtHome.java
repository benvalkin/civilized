package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;

import net.minecraft.server.level.ServerLevel;

public class DropOffImportsAtHome extends ExchangeResourcesAtBuilding {

   public DropOffImportsAtHome() {
      super(WorkStates.DROPPING_OFF_IMPORTS_AT_HOME, 120 * 20, 30 * 20);
   }

   @Override
   protected Optional<Building> findTargetBuilding(ServerLevel level, CivilizedVillager villager) {
      return LoadedBuildings.checkLoaded(villager.getInfo().getHomeBuildingId()).map(LoadedBuilding::getBuilding);
   }

   @Override
   protected void exchangeResources(ServerLevel level, CivilizedVillager villager, long tickTime) {
      dumpInventoryToChests(villager.getLogisticsInventory());

      getSharedCooldowns().startCooldown(Cooldowns.IMPORT_RUN, Duration.of(60, ChronoUnit.SECONDS), tickTime);
   }
}
