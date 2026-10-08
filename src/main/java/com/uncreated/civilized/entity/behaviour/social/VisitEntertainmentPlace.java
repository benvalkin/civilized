package com.uncreated.civilized.entity.behaviour.social;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.VisitBuilding;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ShufflingList;

public class VisitEntertainmentPlace extends VisitBuilding {

   private static final int TAVERN_WEIGHT = 6;
   private static final int TOWN_SQUARE_WEIGHT = 2;
   private static final int OTHER_FOOD_VENDOR_WEIGHT = 2;

   public VisitEntertainmentPlace(float speedModifier) {
      super(
            BehaviourStates.VISITING_ENTERTAINMENT,
            speedModifier,
            Duration.of(5, ChronoUnit.MINUTES),
            Duration.of(30, ChronoUnit.SECONDS));
   }

   @Override
   protected List<Building> chooseBuildings(ServerLevel level, CivilizedVillager villager) {
      ShufflingList<Building> places = new ShufflingList<>();
      for (Building building : ServerBuildingsStore.INSTANCE.findForSettlement(villager.getInfo().getSettlementId())) {
         int weight = entertainmentWeight(building.getBuildingType());
         if (weight > 0)
            places.add(building, weight);
      }

      List<Building> shuffled = new ArrayList<>();
      places.shuffle().forEach(shuffled::add);
      return shuffled;
   }

   private static int entertainmentWeight(BuildingType type) {
      // checked first, since a tavern is also a food vendor
      if (type.is(BuildingTypes.TAVERN))
         return TAVERN_WEIGHT;
      if (type.is(BuildingTypes.TOWN_SQUARE))
         return TOWN_SQUARE_WEIGHT;
      if (type.isFoodVendor())
         return OTHER_FOOD_VENDOR_WEIGHT;
      return 0;
   }
}
