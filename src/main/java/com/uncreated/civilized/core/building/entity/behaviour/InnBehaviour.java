package com.uncreated.civilized.core.building.entity.behaviour;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.recruitment.ImpressionRequirement;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRole;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;
import com.uncreated.civilized.util.random.DailyEventScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class InnBehaviour extends BuildingBehaviour {

   private final DailyEventScheduler eventScheduler;

   protected InnBehaviour(LoadedBuilding entity) {
      super(entity);
      eventScheduler = new DailyEventScheduler(3, 0, 14000, 0.4f);
   }

   @Override
   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      eventScheduler.tick(level, gameTime, () -> trySpawnVisitor(level));
   }

   public static final int MAX_GUESTS = 4;
   private static final int MIN_STAY_DAYS = 2;
   private static final int MAX_STAY_DAYS = 3;
   private static final int DEPARTURE_TIME = 14000;

   private static long chooseDepartureTime(long dayTime, RandomSource random) {
      long startOfDay = dayTime - dayTime % 24000;
      int days = MIN_STAY_DAYS + random.nextInt(MAX_STAY_DAYS - MIN_STAY_DAYS + 1);
      return startOfDay + days * 24000L + DEPARTURE_TIME;
   }

   private static final SimpleWeightedRandomList<VillagerNpcRole> VISITOR_ROLES =
         new SimpleWeightedRandomList.Builder<VillagerNpcRole>().add(VillagerNpcRoles.MIGRANT, 20)
               .add(VillagerNpcRoles.TRAVELLER, 1)
               .build();

   private void trySpawnVisitor(ServerLevel level) {

      if (!innHasRoom(getBuilding()))
         return;

      BlockPos insidePos = getBuilding().getBounds().findRandomInsideFloorBlock(level);

      CivilizedVillager villager =
            EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, insidePos, EntitySpawnReason.EVENT);

      if (villager == null) {
         LOGGER.error("Tried to spawn villager at a Inn, but something went wrong during the entity spawning process.");
         return;
      }

      VillagerNpcRole visitorRole =
            VISITOR_ROLES.getRandomValue(villager.getRandom()).orElse(VillagerNpcRoles.TRAVELLER);
      villager.changeNpcRole(visitorRole);
      villager.getInfo().setHomeBuildingId(getBuilding().getBuildingId());
      VisitorBehaviour.ofVillager(villager)
            .ifPresent(visitor -> visitor.setDepartAt(chooseDepartureTime(level.getDayTime(), villager.getRandom())));
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);

      LOGGER.debug("Villager spawned at Inn: {}", villager.getUUID());
   }

   private static boolean innHasRoom(Building inn) {
      return BuildingUtil.getVisitors(inn, ServerVillagerStore.INSTANCE).size() < MAX_GUESTS;
   }

   public static boolean tryCheckIn(ServerLevel level, CivilizedVillager visitor) {
      VillagerInfo info = visitor.getInfo();
      ImpressionRequirement tier = info.getNpcRole().recruitTier();
      if (tier != ImpressionRequirement.MID && tier != ImpressionRequirement.HIGH)
         return false;

      Optional<Building> townSquare =
            ServerBuildingsStore.INSTANCE.find(info.getHomeBuildingId())
                  .filter(building -> building.getBuildingType().is(BuildingTypes.TOWN_SQUARE));
      if (townSquare.isEmpty())
         return false;

      Optional<Building> inn = findAvailableInn(townSquare.get().getSettlementId());
      if (inn.isEmpty())
         return false;

      info.setHomeBuildingId(inn.get().getBuildingId());
      visitor.getBrain().setMemory(MemoryModuleType.HOME, new GlobalPos(level.dimension(), inn.get().getBlockPos()));
      VisitorBehaviour.ofVillager(visitor)
            .ifPresent(
                  behaviour -> behaviour.setDepartAt(chooseDepartureTime(level.getDayTime(), visitor.getRandom())));
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);
      return true;
   }

   private static Optional<Building> findAvailableInn(UUID settlementId) {
      for (Building building : ServerBuildingsStore.INSTANCE.findForSettlement(settlementId)) {
         if (building.getBuildingType().is(BuildingTypes.INN) && innHasRoom(building))
            return Optional.of(building);
      }
      return Optional.empty();
   }
}
