package com.uncreated.civilized.entity.behaviour.strike;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.VisitBuilding;

import net.minecraft.server.level.ServerLevel;

public class GoToTownSquare extends VisitBuilding {

   public GoToTownSquare(float speedModifier) {
      super(BehaviourStates.GOING_TO_TOWN_SQUARE, speedModifier, Duration.ZERO, Duration.of(30, ChronoUnit.SECONDS));
   }

   @Override
   protected List<Building> chooseBuildings(ServerLevel level, CivilizedVillager villager) {
      // no need to go anywhere if the villager is already there
      return findTownSquare(villager).filter(townSquare -> !townSquare.getBounds().contains(villager.blockPosition()))
            .map(List::of)
            .orElse(List.of());
   }

   public static Optional<Building> findTownSquare(CivilizedVillager villager) {
      if (villager.getInfo().getSettlementId() == null)
         return Optional.empty();

      return ServerBuildingsStore.INSTANCE.findTownSquare(villager.getInfo().getSettlementId());
   }

   public static Optional<LoadedBuilding> findLoadedTownSquare(CivilizedVillager villager) {
      return findTownSquare(villager).flatMap(LoadedBuildings::checkLoaded);
   }
}
