package com.uncreated.civilized.entity.behaviour;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.building.util.WorkAreaUtil;
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
      revalidate(level, villager);
   }

   /**
    * Works out the villager's worksite, then occupation, then home building, in that order. Occupation is decided by
    * their worksite, and where they live depends on their occupation.
    */
   public static void revalidate(ServerLevel level, CivilizedVillager villager) {
      VillagerInfo villagerInfo = villager.getInfo();
      VillagerOccupation oldOccupation = villagerInfo.getOccupation();
      UUID oldHomeId = villagerInfo.getHomeBuildingId();
      UUID oldWorksiteId = villagerInfo.getWorkBuildingId();
      int oldSocialClass = villagerInfo.getSocialClass();

      Optional<Building> worksite = invalidateWorksite(villagerInfo);
      villagerInfo
            .setOccupation(worksite.map(b -> b.getBuildingType().occupation()).orElse(VillagerOccupations.UNEMPLOYED));
      if (worksite.isPresent()) {
         villagerInfo.setWorkBuildingId(worksite.get().getBuildingId());
         villager.getBrain()
               .setMemory(MemoryModuleType.JOB_SITE, new GlobalPos(level.dimension(), worksite.get().getBlockPos()));
      } else {
         villagerInfo.setWorkBuildingId(null);
         villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
      }

      UUID oldWorkAreaId = villagerInfo.getWorkAreaId();
      WorkAreaUtil.reassign(villagerInfo, ServerBuildingsStore.INSTANCE, ServerVillagerStore.INSTANCE);

      // take care not to make villagers think they can work if they are unemployed
      if (villagerInfo.getOccupation().is(VillagerOccupations.UNEMPLOYED))
         villager.getBrain().eraseMemory(AIRegistry.MM_VILLAGER_WORKTIME_OCCUPATION.get());
      else
         villager.getBrain().setMemory(AIRegistry.MM_VILLAGER_WORKTIME_OCCUPATION.get(), villagerInfo.getOccupation());

      Optional<Building> home = invalidateHome(villagerInfo);
      if (home.isPresent()) {
         villagerInfo.setHomeBuildingId(home.get().getBuildingId());
         // social class is not yet an active system
         // villagerInfo.setSocialClass(home.get().getUpgradeLevel());
         villager.getBrain()
               .setMemory(MemoryModuleType.HOME, new GlobalPos(level.dimension(), home.get().getBlockPos()));
      } else {
         villagerInfo.setHomeBuildingId(null);
         villager.getBrain().eraseMemory(MemoryModuleType.HOME);
      }

      boolean jobChanged = !oldOccupation.is(villagerInfo.getOccupation());
      boolean socialClassChanged = oldSocialClass != villagerInfo.getSocialClass();
      boolean homeChanged = !Objects.equals(oldHomeId, villagerInfo.getHomeBuildingId());
      boolean worksiteChanged = !Objects.equals(oldWorksiteId, villagerInfo.getWorkBuildingId());
      boolean workAreaChanged = !Objects.equals(oldWorkAreaId, villagerInfo.getWorkAreaId());
      if (jobChanged || homeChanged || worksiteChanged || workAreaChanged) {
         ServerVillagerStore.INSTANCE.setDirty();
         ServerVillagerStore.INSTANCE.replicateChange(villagerInfo, StoreOperation.UPDATE);
      }
      if (homeChanged || worksiteChanged) {
         // update the buildings they left and the ones they joined
         BuildingUtil.serverRefreshAndReplicate(
               oldHomeId,
               oldWorksiteId,
               villagerInfo.getHomeBuildingId(),
               villagerInfo.getWorkBuildingId());
      }
      if (jobChanged || socialClassChanged) {
         villager.refreshBrain(level);
         villager.updateClothing();
      }
   }

   private static Optional<Building> invalidateWorksite(VillagerInfo villagerInfo) {
      if (!villagerInfo.getNpcRole().is(VillagerNpcRoles.WORKER))
         return Optional.empty();

      return ServerBuildingsStore.INSTANCE.find(villagerInfo.getWorkBuildingId())
            .filter(b -> b.getBuildingType().offersWork())
            .filter(b -> b.getSettlementId().equals(villagerInfo.getSettlementId()));
   }

   private static Optional<Building> invalidateHome(VillagerInfo villagerInfo) {

      if (villagerInfo.getNpcRole().is(VillagerNpcRoles.WORKER)) {
         return invalidateHomeForWorker(villagerInfo);
      } else if (villagerInfo.getNpcRole().is(VillagerNpcRoles.SPOUSE)) {
         return invalidateHomeForSpouse(villagerInfo);
      }

      return Optional.empty();
   }

   private static Optional<Building> invalidateHomeForWorker(VillagerInfo worker) {

      Optional<Building> currentHome = ServerBuildingsStore.INSTANCE.find(worker.getHomeBuildingId());
      VillagerOccupation occupation = worker.getOccupation();

      // workers live in the housing that comes with their job if it exists
      if (currentHome.isPresent() && currentHome.get().getBuildingType().isHousingFor(occupation))
         return currentHome;

      Optional<Building> housing =
            BuildingUtil.findFreeHousingFor(
                  worker.getSettlementId(),
                  occupation,
                  ServerBuildingsStore.INSTANCE,
                  ServerVillagerStore.INSTANCE);
      if (housing.isPresent())
         return housing;

      // Otherwise, they are no longer staying in a home that corresponds to their job.
      // They can stay continue to stay here unless their this home has now been given to a different resident worker.
      if (currentHome.isPresent() && !BuildingUtil.hasResidentWorker(currentHome.get(), ServerVillagerStore.INSTANCE))
         return currentHome;

      // If they're homeless, try to find a new worker home.
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

   private static Optional<Building> invalidateHomeForSpouse(VillagerInfo spouse) {

      Optional<VillagerInfo> mainHomeOwner = ServerVillagerStore.INSTANCE.find(spouse.getPartnerId());
      if (mainHomeOwner.isPresent()) {

         // spouses inherits the last name of the main villager
         spouse.setLastName(mainHomeOwner.get().getLastName());

         // try move to wherever the spouse's partner is staying
         Optional<Building> building = ServerBuildingsStore.INSTANCE.find(mainHomeOwner.get().getHomeBuildingId());
         if (building.isPresent() && building.get().getBuildingType().isSpouseResidence())
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
}
