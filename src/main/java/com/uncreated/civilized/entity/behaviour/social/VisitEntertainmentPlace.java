package com.uncreated.civilized.entity.behaviour.social;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.settlement.festival.Festival;
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

   private static final int MIN_TAVERN_LINGER_TICKS = 60 * 20;
   private static final int MAX_TAVERN_LINGER_TICKS = 4 * 60 * 20;
   private static final int MIN_TOWN_SQUARE_LINGER_TICKS = 30 * 20;
   private static final int MAX_TOWN_SQUARE_LINGER_TICKS = 2 * 60 * 20;
   private static final int MIN_FOOD_VENDOR_LINGER_TICKS = 30 * 20;
   private static final int MAX_FOOD_VENDOR_LINGER_TICKS = 60 * 20;

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
      // visitors don't belong to a settlement, but can still visit the one they're staying at
      UUID settlementId = Festival.findHostSettlementId(villager.getInfo());
      if (settlementId == null)
         return List.of();

      // visitors count too, since they also take up room at the building
      List<UUID> patrons =
            Stream.concat(
                  ServerVillagerStore.INSTANCE.getCitizens(settlementId).stream(),
                  ServerVillagerStore.INSTANCE.getVisitors(settlementId).stream())
                  .map(VillagerInfo::getVillagerId)
                  .toList();

      ShufflingList<Building> places = new ShufflingList<>();
      for (Building building : ServerBuildingsStore.INSTANCE.findForSettlement(settlementId)) {
         int weight = entertainmentWeight(building, villager, level.dayTime());
         if (weight <= 0)
            continue;

         Integer maxRecreationVisitors = building.getBuildingType().maxRecreationVisitors();
         if (maxRecreationVisitors != null) {
            AABB visitingBounds = building.getBounds().getEncapsulatingAABB().inflate(1);

            long numberOfVisitors =
                  LoadedVillagers.matching(patrons)
                        .stream()
                        .filter(v -> visitingBounds.contains(v.position()))
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

   @Override
   protected int lingerTicks(CivilizedVillager villager, Building building) {
      BuildingType buildingType = building.getBuildingType();

      if (buildingType.is(BuildingTypes.TAVERN))
         return villager.getRandom().nextIntBetweenInclusive(MIN_TAVERN_LINGER_TICKS, MAX_TAVERN_LINGER_TICKS);

      if (buildingType.is(BuildingTypes.TOWN_SQUARE))
         return villager.getRandom()
               .nextIntBetweenInclusive(MIN_TOWN_SQUARE_LINGER_TICKS, MAX_TOWN_SQUARE_LINGER_TICKS);

      if (buildingType.isFoodVendor())
         return villager.getRandom()
               .nextIntBetweenInclusive(MIN_FOOD_VENDOR_LINGER_TICKS, MAX_FOOD_VENDOR_LINGER_TICKS);

      // e.g. their own home, where they'd rather go back to strolling around the settlement
      return 0;
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
