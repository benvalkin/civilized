package com.uncreated.civilized.core.building.util;

import static com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab.MAX_ASSIGNED_WORKERS;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRole;
import com.uncreated.civilized.core.villagerinfo.VillagerStore;

public class BuildingUtil {

   public static List<VillagerInfo> getResidents(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId()).stream().filter(v -> v.isOccupantOf(building)).toList();
   }

   public static List<VillagerInfo> getAssignedWorkers(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> v.isAssignedWorkerOf(building))
            .toList();
   }

   public static boolean isBuildingFull(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> v.getNpcRoles().contains(VillagerNpcRole.WORKER) && v.isOccupantOf(building))
            .count() >= MAX_ASSIGNED_WORKERS;
   }

   public static boolean isWorksiteFull(Building building, VillagerStore store) {
      return store.getCitizens(building.getSettlementId())
            .stream()
            .filter(v -> v.isAssignedWorkerOf(building))
            .count() == MAX_ASSIGNED_WORKERS;
   }

   public static Optional<Building> findUnoccupiedHome(
         UUID settlementId,
         BuildingStore buildingStore,
         VillagerStore villagerStore,
         boolean includeTemporaryHomes) {

      return buildingStore.findForSettlement(settlementId)
            .stream()
            .filter(
                  b -> b.getSettlementId().equals(settlementId)
                        && (includeTemporaryHomes ? b.getBuildingType().isResidence()
                              : b.getBuildingType().isResidence())
                        && !isBuildingFull(b, villagerStore))
            .findFirst();
   }

   public static Optional<Building> findUnoccupiedHome(
         UUID settlementId,
         BuildingType requiredBuildingType,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {

      return buildingStore.findForSettlement(settlementId)
            .stream()
            .filter(
                  b -> b.getSettlementId().equals(settlementId) && b.getBuildingType() == requiredBuildingType
                        && !isBuildingFull(b, villagerStore))
            .findFirst();
   }

   public static Optional<Building> findUnoccupiedWorksite(
         UUID settlementId,
         Predicate<BuildingType> filter,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {

      return buildingStore.findForSettlement(settlementId)
            .stream()
            .filter(
                  b -> b.getSettlementId().equals(settlementId) && filter.test(b.getBuildingType())
                        && !isWorksiteFull(b, villagerStore))
            .findFirst();
   }
}
