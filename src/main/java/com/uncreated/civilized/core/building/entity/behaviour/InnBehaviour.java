package com.uncreated.civilized.core.building.entity.behaviour;

import java.util.Optional;
import java.util.UUID;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
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

   private static final int MIN_STAY_DAYS = 2;
   private static final int MAX_STAY_DAYS = 4;
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

      if (isFull())
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
      VisitorBehaviour.tryGet(villager)
            .ifPresent(visitor -> visitor.setDepartAt(chooseDepartureTime(level.getDayTime(), villager.getRandom())));
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);

      LOGGER.debug("Villager spawned at Inn: {}", villager.getUUID());
   }

   public boolean tryCheckIn(ServerLevel level, CivilizedVillager visitor) {
      VillagerInfo info = visitor.getInfo();
      ImpressionRequirement tier = info.getNpcRole().recruitTier();
      if (tier != ImpressionRequirement.MID && tier != ImpressionRequirement.HIGH)
         return false; // low tier visitors cannot stay in the in

      info.setHomeBuildingId(getBuilding().getBuildingId());
      visitor.getBrain()
            .setMemory(MemoryModuleType.HOME, new GlobalPos(level.dimension(), getBuilding().getBlockPos()));
      VisitorBehaviour.tryGet(visitor)
            .ifPresent(
                  behaviour -> behaviour.setDepartAt(chooseDepartureTime(level.getDayTime(), visitor.getRandom())));
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);
      return true;
   }

   public static Optional<InnBehaviour> findAvailableInn(UUID settlementId) {
      return ServerBuildingsStore.INSTANCE.findForSettlement(settlementId)
            .stream()
            .map(LoadedBuildings::checkLoaded)
            .flatMap(Optional::stream)
            .filter(inn -> inn.getBehaviour() instanceof InnBehaviour innBehaviour && !innBehaviour.isFull())
            .map(b -> (InnBehaviour) b.getBehaviour())
            .findFirst();
   }

   public int getMaxVisitors() {
      int upgradeLevel = getBuilding().getUpgradeLevel();
      if (upgradeLevel <= 1)
         return 3;
      if (upgradeLevel == 2)
         return 5;
      return 7;
   }

   public boolean isFull() {
      return BuildingUtil.getVisitors(getBuilding(), ServerVillagerStore.INSTANCE).size() >= getMaxVisitors();
   }
}
