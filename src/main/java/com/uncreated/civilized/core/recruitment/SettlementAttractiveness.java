package com.uncreated.civilized.core.recruitment;

import static com.uncreated.civilized.core.dialogue.DialogueLines.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.VillagerStore;

public final class SettlementAttractiveness {

   private static final String SOURCES_FILE = "recruitment/attractiveness";

   private static final int PER_TOWN_HALL_LEVEL = 5;
   private static final int JOBS_AVAILABLE = 5;
   private static final int TAVERN = 8;
   private static final int CHURCH = 8;
   private static final int INN = 5;
   private static final int CITIZENS_PER_POINT = 2;
   private static final int MAX_FROM_POPULATION = 20;

   private SettlementAttractiveness() {
   }

   public static List<AttractivenessSource> compute(
         UUID settlementId,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {
      List<AttractivenessSource> sources = new ArrayList<>();

      Set<Building> buildings = buildingStore.findForSettlement(settlementId);

      addIfPresent(
            buildings,
            sources,
            BuildingTypes.TOWN_HALL,
            b -> new AttractivenessSource(
                  text(SOURCES_FILE, "town_hall", b.getUpgradeLevel()),
                  b.getUpgradeLevel() * PER_TOWN_HALL_LEVEL));

      if (BuildingUtil.findUnoccupiedWorksite(settlementId, BuildingType::isWorkBuilding, buildingStore, villagerStore)
            .isPresent())
         sources.add(source("jobs_available", JOBS_AVAILABLE));

      addIfPresent(buildings, sources, BuildingTypes.TAVERN, b -> source("tavern", TAVERN));
      addIfPresent(buildings, sources, BuildingTypes.CHURCH, b -> source("church", CHURCH));
      addIfPresent(buildings, sources, BuildingTypes.INN, b -> source("inn", INN));

      int citizens = villagerStore.getCitizens(settlementId).size();
      int fromPopulation = Math.min(citizens / CITIZENS_PER_POINT, MAX_FROM_POPULATION);
      if (fromPopulation > 0)
         sources.add(new AttractivenessSource(text(SOURCES_FILE, "population", citizens), fromPopulation));

      return sources;
   }

   public static int total(List<AttractivenessSource> sources) {
      int total = 0;
      for (AttractivenessSource source : sources)
         total += source.value();
      return total;
   }

   private static void addIfPresent(
         Set<Building> buildings,
         List<AttractivenessSource> sources,
         BuildingType type,
         Function<Building, AttractivenessSource> createSource) {
      for (Building building : buildings) {
         if (building.getBuildingType().is(type)) {
            sources.add(createSource.apply(building));
            return;
         }
      }
   }

   public static AttractivenessSource source(String name, int value) {
      return new AttractivenessSource(text(SOURCES_FILE, name), value);
   }
}
