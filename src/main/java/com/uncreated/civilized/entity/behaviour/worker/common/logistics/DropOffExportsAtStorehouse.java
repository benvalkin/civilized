package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;

import net.minecraft.server.level.ServerLevel;

public class DropOffExportsAtStorehouse extends ExchangeResourcesAtBuilding {

   private static final Logger LOGGER = LogUtils.getLogger();

   private LoadedSettlement settlement;

   public DropOffExportsAtStorehouse() {
      super(WorkStates.DROPPING_OFF_EXPORTS_AT_STOREHOUSE, 120 * 20, 30 * 20);
   }

   @Override
   protected Optional<Building> findTargetBuilding(ServerLevel level, CivilizedVillager villager) {
      return ServerBuildingsStore.INSTANCE.findStorehouse(villager.getInfo().getSettlementId())
            .flatMap(LoadedBuildings::checkLoaded)
            .map(LoadedBuilding::getBuilding);
   }

   @Override
   protected void exchangeResources(ServerLevel level, CivilizedVillager villager, long tickTime) {

      dumpInventoryToChests(villager.getLogisticsInventory());

      // since villager is already at the storehouse, might as well import stuff
      getStateMachine().queueActionOnce(WorkStates.FETCHING_IMPORTS_FROM_STOREHOUSE);

      getSharedCooldowns().startCooldown(Cooldowns.EXPORT_RUN, Duration.of(3, ChronoUnit.MINUTES), tickTime);
   }
}
