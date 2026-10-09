package com.uncreated.civilized.entity.behaviour;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class InvalidateImportantLocations extends RecurringIntervalBehaviour<CivilizedVillager> {

   private Logger LOGGER = LogUtils.getLogger();

   public InvalidateImportantLocations() {
      super(ImmutableMap.of());
   }

   @Override
   protected long getIntervalDurationSeconds() {
      return 20;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTicks) {
      super.start(level, villager, gameTicks);

      VillagerInfo villagerInfo = villager.getInfo();
      VillagerOccupation oldOccupation = villagerInfo.getOccupation();
      Optional<Building> oldHome = ServerBuildingsStore.INSTANCE.find(villagerInfo.getHomeBuildingId());
      Optional<Building> oldWorksite = ServerBuildingsStore.INSTANCE.find(villagerInfo.getPrimaryWorksiteId());
      int oldSocialClass = villagerInfo.getSocialClass();
      Optional<Building> home = invalidateHome(villagerInfo);
      if (home.isPresent()) {

         if (villagerInfo.getNpcRole().is(VillagerNpcRoles.WORKER))
            villagerInfo.setOccupation(home.get().getBuildingType().occupation());
         else
            villagerInfo.setOccupation(VillagerOccupations.UNEMPLOYED);

         villagerInfo.setHomeBuildingId(home.get().getBuildingId());
         villagerInfo.setSocialClass(home.get().getUpgradeLevel());
         villager.getBrain()
               .setMemory(MemoryModuleType.HOME, new GlobalPos(level.dimension(), home.get().getBlockPos()));

         if (!villagerInfo.getOccupation().is(VillagerOccupations.UNEMPLOYED)) // take care not to make villagers think
            // they can work if they are unemployed
            villager.getBrain()
                  .setMemory(AIRegistry.MM_VILLAGER_WORKTIME_OCCUPATION.get(), villagerInfo.getOccupation());
      } else {
         villagerInfo.setOccupation(VillagerOccupations.UNEMPLOYED);
         villager.getBrain().eraseMemory(MemoryModuleType.HOME);
         villager.getBrain().eraseMemory(AIRegistry.MM_VILLAGER_WORKTIME_OCCUPATION.get());
      }

      Optional<Building> worksite = invalidateWorksite(villagerInfo, level);
      if (worksite.isPresent()) {
         villagerInfo.setPrimaryWorksiteId(worksite.get().getBuildingId());
         villager.getBrain()
               .setMemory(MemoryModuleType.JOB_SITE, new GlobalPos(level.dimension(), worksite.get().getBlockPos()));
      } else {
         villagerInfo.setPrimaryWorksiteId(null);
         villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
      }

      boolean jobChanged = oldOccupation != villagerInfo.getOccupation();
      boolean socialClassChanged = oldSocialClass != villagerInfo.getSocialClass();
      boolean homeChanged =
            !Objects.equals(oldHome.map(Building::getBuildingId).orElse(null), villagerInfo.getHomeBuildingId());
      boolean worksiteChanged =
            !Objects.equals(oldWorksite.map(Building::getBuildingId).orElse(null), villagerInfo.getPrimaryWorksiteId());
      boolean villagerChanged = jobChanged || homeChanged || worksiteChanged;
      if (villagerChanged) {
         ServerVillagerStore.INSTANCE.setDirty();
         ServerVillagerStore.INSTANCE.replicateChange(villagerInfo, StoreOperation.UPDATE);
      }
      if (homeChanged) {
         oldHome.ifPresent(b -> {
            b.getOccupantIds().remove(villagerInfo.getVillagerId());
            ServerBuildingsStore.INSTANCE.replicateChange(b, StoreOperation.UPDATE);
         });
         home.ifPresent(b -> {
            b.getOccupantIds().add(villagerInfo.getVillagerId());
            ServerBuildingsStore.INSTANCE.replicateChange(b, StoreOperation.UPDATE);
         });
      }
      if (jobChanged || socialClassChanged) {
         villager.refreshBrain(level);
         villager.updateClothing();
      }
      if (worksiteChanged) {
         oldWorksite.ifPresent(b -> {
            b.getOccupantIds().remove(villagerInfo.getVillagerId());
            ServerBuildingsStore.INSTANCE.replicateChange(b, StoreOperation.UPDATE);
         });
         worksite.ifPresent(b -> {
            b.getOccupantIds().add(villagerInfo.getVillagerId());
            ServerBuildingsStore.INSTANCE.replicateChange(b, StoreOperation.UPDATE);
         });
      }
   }

   private Optional<Building> invalidateHome(VillagerInfo villagerInfo) {

      if (villagerInfo.getNpcRole().is(VillagerNpcRoles.WORKER)) {
         return invalidateHomeForWorker(villagerInfo);
      } else if (villagerInfo.getNpcRole().is(VillagerNpcRoles.SPOUSE)) {
         return invalidateHomeForSpouse(villagerInfo);
      }

      return Optional.empty();
   }

   private Optional<Building> invalidateHomeForWorker(VillagerInfo worker) {

      Optional<Building> currentHome = ServerBuildingsStore.INSTANCE.find(worker.getHomeBuildingId());
      if (currentHome.isPresent())
         return currentHome;

      // If their current home no longer exists, or they are already homeless, try to find a new worker home.
      Optional<Building> newWorkerHome =
            BuildingUtil.findUnoccupiedWorkerHome(
                  worker.getSettlementId(),
                  ServerBuildingsStore.INSTANCE,
                  ServerVillagerStore.INSTANCE);
      if (newWorkerHome.isPresent())
         return newWorkerHome;

      // Fallback option is to live in an alternative home (e.g. a town house)
      Optional<Building> alternativeHome =
            BuildingUtil.findEmptyAlternativeHomeForWorker(worker.getSettlementId(), ServerBuildingsStore.INSTANCE);
      if (alternativeHome.isPresent())
         return alternativeHome;

      // or otherwise the town hall
      Optional<Building> townHall = ServerBuildingsStore.INSTANCE.findTownHall(worker.getSettlementId());
      if (townHall.isPresent())
         return townHall;

      // Otherwise, they will roam around homeless at night :(
      return Optional.empty();
   }

   private Optional<Building> invalidateHomeForSpouse(VillagerInfo spouse) {

      Optional<VillagerInfo> mainHomeOwner = ServerVillagerStore.INSTANCE.find(spouse.getPartnerId());
      if (mainHomeOwner.isPresent()) {
         // try move to wherever the spouse's partner is staying
         Optional<Building> building = ServerBuildingsStore.INSTANCE.find(mainHomeOwner.get().getHomeBuildingId());
         if (building.isPresent())
            return building;
      }

      // if we cannot stay where the partner is staying (partner is missing, or partner is homeless), they are allowed
      // to remain in whatever building they're currently staying at, provided it doesn't now belong to another worker
      // who is not their partner
      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(spouse.getHomeBuildingId());
      if (building.isPresent()) {
         boolean existingResidenceOccupiedByStranger =
               BuildingUtil.getOccupants(building.get(), ServerVillagerStore.INSTANCE)
                     .stream()
                     .anyMatch(other -> other.getNpcRole().is(VillagerNpcRoles.WORKER) && !spouse.isPartnerOf(other));

         if (!existingResidenceOccupiedByStranger)
            return building;
      }

      // if they have no partner, their partner is homeless, and they cannot stay in their current building, spouses can
      // live in any empty building where spouses can usually live
      Optional<Building> alternative =
            BuildingUtil.findEmptyAlternativeHomeForSpouse(
                  spouse.getSettlementId(),
                  ServerBuildingsStore.INSTANCE,
                  ServerVillagerStore.INSTANCE);
      if (alternative.isPresent())
         return alternative;

      // if there are not even any viable empty buildings, they will still in the town hall
      Optional<Building> townHall = ServerBuildingsStore.INSTANCE.findTownHall(spouse.getSettlementId());
      if (townHall.isPresent())
         return townHall;

      // Otherwise, they will roam around homeless at night :(
      return Optional.empty();
   }

   private Optional<Building> invalidateWorksite(VillagerInfo villagerInfo, ServerLevel level) {

      if (villagerInfo.getOccupation().is(VillagerOccupations.UNEMPLOYED)
            || !villagerInfo.getNpcRole().is(VillagerNpcRoles.WORKER))
         return Optional.empty();

      Optional<Building> currentWorksite = ServerBuildingsStore.INSTANCE.find(villagerInfo.getPrimaryWorksiteId());
      if (currentWorksite.isPresent())
         return currentWorksite;

      return BuildingUtil.findUnoccupiedWorksite(
            villagerInfo.getSettlementId(),
            villagerInfo.getOccupation().validWorksite(),
            ServerBuildingsStore.INSTANCE,
            ServerVillagerStore.INSTANCE);
   }
}
