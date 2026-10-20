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
            .filter(b -> b.getBuildingType().isWorkBuilding())
            .filter(b -> b.getSettlementId().equals(villagerInfo.getSettlementId()));
   }

   private static Optional<Building> invalidateHome(VillagerInfo villagerInfo) {

      boolean isCitizen =
            villagerInfo.getNpcRole().is(VillagerNpcRoles.WORKER)
                  || villagerInfo.getNpcRole().is(VillagerNpcRoles.SPOUSE);
      if (!isCitizen)
         return Optional.empty();

      // spouses inherit their partner's last name
      if (villagerInfo.getNpcRole().is(VillagerNpcRoles.SPOUSE))
         ServerVillagerStore.INSTANCE.find(villagerInfo.getPartnerId())
               .ifPresent(partner -> villagerInfo.setLastName(partner.getLastName()));

      if (villagerInfo.getOccupation().is(VillagerOccupations.SOLDIER))
         return invalidateHomeForSoldier(villagerInfo);
      else
         return invalidateHomeForCivilian(villagerInfo);
   }

   private static Optional<Building> invalidateHomeForCivilian(VillagerInfo civilian) {

      // villagers live with their family when they can
      Optional<Building> familyHome = findFamilyHome(civilian);
      if (familyHome.isPresent())
         return familyHome;

      // otherwise, workers live where they work
      Optional<Building> workBuilding = findWorkBuildingResidence(civilian);
      if (workBuilding.isPresent())
         return workBuilding;

      // otherwise, they can stay wherever they're staying, provided it doesn't now belong to a stranger who has been
      // assigned to work in their current home
      Optional<Building> currentHome = ServerBuildingsStore.INSTANCE.find(civilian.getHomeBuildingId());
      if (currentHome.isPresent() && !isClaimedByAnotherHousehold(currentHome.get(), civilian))
         return currentHome;

      // if they can't stay there, any empty family home will do (e.g. a town house)
      Optional<Building> emptyHome =
            BuildingUtil.findEmptyGeneralFamilyResidence(
                  civilian.getSettlementId(),
                  ServerBuildingsStore.INSTANCE,
                  ServerVillagerStore.INSTANCE);
      if (emptyHome.isPresent())
         return emptyHome;

      // if there are not even any viable empty homes, they will stay in the town hall
      Optional<Building> townHall = ServerBuildingsStore.INSTANCE.findTownHall(civilian.getSettlementId());
      if (townHall.isPresent())
         return townHall;

      // Otherwise, they will roam around homeless at night :(
      return Optional.empty();
   }

   private static Optional<Building> invalidateHomeForSoldier(VillagerInfo soldier) {

      // soldiers live with their family when they can. Their post is never their family's home.
      Optional<Building> familyHome = findFamilyHome(soldier);
      if (familyHome.isPresent())
         return familyHome;

      // a soldier who has a family but no suitable family home (can happen when two soldiers are partners) tries to
      // move into an empty family home
      if (soldier.getPartnerId() != null) {
         Optional<Building> emptyHome =
               BuildingUtil.findEmptyGeneralFamilyResidence(
                     soldier.getSettlementId(),
                     ServerBuildingsStore.INSTANCE,
                     ServerVillagerStore.INSTANCE);
         if (emptyHome.isPresent())
            return emptyHome;
      }

      // otherwise, they stay live at their assigned garrison
      Optional<Building> post = findWorkBuildingResidence(soldier);
      if (post.isPresent())
         return post;

      // garrisons should always have beds, but just in case
      Optional<Building> townHall = ServerBuildingsStore.INSTANCE.findTownHall(soldier.getSettlementId());
      if (townHall.isPresent())
         return townHall;

      return Optional.empty();
   }

   /**
    * Where the villager's family lives together: whichever of the villager's and their partner's homes is a family
    * residence that no other household has claimed. When both are, the one with the higher upgrade level wins.
    */
   private static Optional<Building> findFamilyHome(VillagerInfo villager) {

      Optional<VillagerInfo> partner = ServerVillagerStore.INSTANCE.find(villager.getPartnerId());
      if (partner.isEmpty())
         return Optional.empty();

      Optional<Building> ownHome = findUnclaimedFamilyResidence(villager.getHomeBuildingId(), villager);
      Optional<Building> partnerHome = findUnclaimedFamilyResidence(partner.get().getHomeBuildingId(), villager);

      if (ownHome.isPresent() && partnerHome.isPresent())
         return Optional.of(chooseFamilyHome(ownHome.get(), partnerHome.get()));
      if (partnerHome.isPresent())
         return partnerHome;
      return ownHome;
   }

   private static Optional<Building> findUnclaimedFamilyResidence(UUID buildingId, VillagerInfo villager) {

      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(buildingId);
      if (building.isEmpty() || !building.get().getBuildingType().isFamilyResidence())
         return Optional.empty();

      if (isClaimedByAnotherHousehold(building.get(), villager))
         return Optional.empty();

      return building;
   }

   /**
    * Given two viable family buildings belonging to two partners, chooses the one with the highest upgrade level
    * (higher level = probably better). If it's a tie, one will be chosen deterministically by comparing the two
    * buildingIds.
    */
   private static Building chooseFamilyHome(Building partner1building, Building partner2building) {

      if (partner1building.getUpgradeLevel() != partner2building.getUpgradeLevel())
         return partner1building.getUpgradeLevel() > partner2building.getUpgradeLevel() ? partner1building
               : partner2building;

      return partner1building.getBuildingId().compareTo(partner2building.getBuildingId()) <= 0 ? partner1building
            : partner2building;
   }

   /**
    *
    * @return the specified worker's assigned work building, provided that it is a residence where they can live.
    */
   private static Optional<Building> findWorkBuildingResidence(VillagerInfo worker) {

      Optional<Building> workBuilding = ServerBuildingsStore.INSTANCE.find(worker.getWorkBuildingId());
      if (workBuilding.isEmpty() || !workBuilding.get().getBuildingType().isWorkerResidence())
         return Optional.empty();

      return workBuilding;
   }

   /**
    * Whether a household other than the villager's family has a claim to the building, either because they already live
    * there as residents, or because they are assigned workers and the building is a work building. Work buildings are
    * only for assigned workers and their family).
    */
   private static boolean isClaimedByAnotherHousehold(Building building, VillagerInfo villager) {

      if (building.getBuildingType().isWorkBuilding()) {
         for (VillagerInfo worker : BuildingUtil.getAssignedWorkers(building, ServerVillagerStore.INSTANCE)) {
            if (!isSameHousehold(villager, worker))
               return true;
         }
         return false;
      }

      for (VillagerInfo resident : BuildingUtil.getOccupants(building, ServerVillagerStore.INSTANCE)) {
         if (!isSameHousehold(villager, resident))
            return true;
      }
      return false;
   }

   private static boolean isSameHousehold(VillagerInfo villager, VillagerInfo other) {
      return villager.getVillagerId().equals(other.getVillagerId()) || villager.isPartnerOf(other);
   }
}
