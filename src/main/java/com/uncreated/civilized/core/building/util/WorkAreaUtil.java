package com.uncreated.civilized.core.building.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerStore;

public final class WorkAreaUtil {

   private WorkAreaUtil() {
   }

   public static Optional<Building> findWorkArea(VillagerInfo worker, BuildingStore store) {
      Optional<Building> building = store.find(worker.getWorkAreaId());
      if (building.isEmpty())
         return Optional.empty();

      if (!canWork(worker, building.get()))
         return Optional.empty();

      return building;
   }

   public static List<VillagerInfo> getWorkers(Building workArea, VillagerStore store) {
      return store.getCitizens(workArea.getSettlementId())
            .stream()
            .filter(worker -> workArea.getBuildingId().equals(worker.getWorkAreaId()))
            .filter(worker -> canWork(worker, workArea))
            .toList();
   }

   public static boolean reassign(VillagerInfo worker, BuildingStore buildingStore, VillagerStore villagerStore) {
      UUID newWorkAreaId = null;
      if (findWorkArea(worker, buildingStore).isPresent())
         return false;
      else {
         Optional<Building> worksite = buildingStore.find(worker.getWorkBuildingId());
         if (worksite.isPresent()) {
            Optional<Building> nearestWorkArea =
                  buildingStore.findForSettlement(worker.getSettlementId())
                        .stream()
                        .filter(area -> canWork(worker, area))
                        .filter(area -> hasRoom(area, villagerStore))
                        .min(
                              Comparator
                                    .comparingDouble(area -> area.getBlockPos().distSqr(worksite.get().getBlockPos())));

            newWorkAreaId = nearestWorkArea.map(Building::getBuildingId).orElse(null);
         }
      }

      if (Objects.equals(newWorkAreaId, worker.getWorkAreaId()))
         return false;

      worker.setWorkAreaId(newWorkAreaId);
      return true;
   }

   public static boolean canWork(VillagerInfo worker, Building workArea) {
      return worker.getOccupation().isValidWorkArea(workArea.getBuildingType())
            && workArea.getBuildingType().occupation().equals(worker.getOccupation())
            && workArea.getSettlementId().equals(worker.getSettlementId());
   }

   public static List<VillagerInfo> assign(
         VillagerInfo worker,
         Building workArea,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {
      if (workArea.getBuildingId().equals(worker.getWorkAreaId()))
         return List.of();

      List<VillagerInfo> changed = new ArrayList<>();
      Optional<Building> oldWorkArea = findWorkArea(worker, buildingStore);

      List<VillagerInfo> currentWorkers = getWorkers(workArea, villagerStore);
      boolean currentWorkerWillBeDisplaced = isABoolean(workArea, currentWorkers);
      if (currentWorkerWillBeDisplaced) {
         VillagerInfo displaced = currentWorkers.getFirst();
         // if the assigned worker moved from a different worksite, the displaced worker can take it over (they have the
         // same occupation)
         if (oldWorkArea.isPresent())
            displaced.setWorkAreaId(oldWorkArea.get().getBuildingId());
         else
            displaced.setWorkAreaId(null);
         changed.add(displaced);
      }

      worker.setWorkAreaId(workArea.getBuildingId());
      changed.add(worker);
      return changed;
   }

   private static boolean isABoolean(Building workArea, List<VillagerInfo> currentWorkers) {
      return !currentWorkers.isEmpty() && currentWorkers.size() >= workArea.getMaxWorkers();
   }

   private static boolean hasRoom(Building workArea, VillagerStore villagerStore) {
      return getWorkers(workArea, villagerStore).size() < workArea.getMaxWorkers();
   }
}
