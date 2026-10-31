package com.uncreated.civilized.entity.behaviour.social;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.VisitBuilding;
import com.uncreated.civilized.entity.equipment.VillagerEquipment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.phys.AABB;

public class VisitEntertainmentPlace extends VisitBuilding {

   private static final int TAVERN_WEIGHT = 5;
   private static final int TOWN_SQUARE_WEIGHT = 2;
   private static final int OTHER_FOOD_VENDOR_WEIGHT = 2;
   private static final int HOME_WEIGHT = 2;

   public VisitEntertainmentPlace(float speedModifier) {
      super(
            BehaviourStates.VISITING_ENTERTAINMENT,
            speedModifier,
            Duration.of(5, ChronoUnit.MINUTES),
            Duration.of(30, ChronoUnit.SECONDS));
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      long todayTime = level.getDayTime() % 24000;
      if (!(todayTime >= CivilizedVillager.WORK_FINISH_DAY_TIME && todayTime <= CivilizedVillager.REST_TIME))
         return false; // can only go visit entertainment after work

      return super.checkExtraStartConditions(level, villager);
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      villager.getEquipment().unequip(VillagerEquipment.COMBAT_SLOTS);
   }

   @Override
   protected List<Building> chooseBuildings(ServerLevel level, CivilizedVillager villager) {

      List<UUID> citizens =
            ServerVillagerStore.INSTANCE.getCitizens(villager.getInfo().getSettlementId())
                  .stream()
                  .map(VillagerInfo::getVillagerId)
                  .toList();

      ShufflingList<Building> places = new ShufflingList<>();
      for (Building building : ServerBuildingsStore.INSTANCE.findForSettlement(villager.getInfo().getSettlementId())) {
         int weight = entertainmentWeight(building, villager, level.dayTime());
         if (weight <= 0)
            continue;

         Integer maxRecreationVisitors = building.getBuildingType().maxRecreationVisitors();
         if (maxRecreationVisitors != null) {
            AABB visitingBounds = building.getBounds().getEncapsulatingAABB().inflate(1);

            long numberOfVisitors =
                  LoadedVillagers.matching(citizens)
                        .stream()
                        .filter(v -> visitingBounds.contains(villager.position()))
                        .count();

            if (numberOfVisitors >= maxRecreationVisitors)
               continue;
         }

         places.add(building, weight);

      }

      List<Building> shuffled = new ArrayList<>();
      places.shuffle().forEach(shuffled::add);
      return shuffled;
   }

   private static int entertainmentWeight(Building building, CivilizedVillager villager, long dayTime) {

      BuildingType buildingType = building.getBuildingType();

      long todayTime = dayTime % 24000;

      // checked first, since a tavern is also a food vendor
      if (buildingType.is(BuildingTypes.TAVERN)) {

         if (todayTime >= 12000 && todayTime <= CivilizedVillager.REST_TIME)
            return TAVERN_WEIGHT;

         return 0;
      }
      if (buildingType.is(BuildingTypes.TOWN_SQUARE)) {
         return TOWN_SQUARE_WEIGHT;
      }

      if (buildingType.isFoodVendor())
         return OTHER_FOOD_VENDOR_WEIGHT;

      if (building.getBuildingId().equals(villager.getInfo().getHomeBuildingId()))
         return HOME_WEIGHT;

      return 0;
   }
}
