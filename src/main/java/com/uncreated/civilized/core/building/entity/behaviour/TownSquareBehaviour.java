package com.uncreated.civilized.core.building.entity.behaviour;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRole;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;
import com.uncreated.civilized.util.random.DailyEventScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntitySpawnReason;

public class TownSquareBehaviour extends BuildingBehaviour {

   private final DailyEventScheduler eventScheduler;

   public TownSquareBehaviour(LoadedBuilding entity) {
      super(entity);
      eventScheduler = new DailyEventScheduler(7, 0, 6000, 0.1f);
   }

   private static final int MAX_VISITORS = 12;
   private static final int SPAWN_SPOT_ATTEMPTS = 16;

   // visitors may leave at any time from 5pm, and are guaranteed to be gone by 10pm
   private static final int EARLIEST_DEPARTURE_TIME = 11000;
   private static final int LATEST_DEPARTURE_TIME = 16000;

   @Override
   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      eventScheduler.tick(level, gameTime, () -> trySpawnVisitor(level));
   }

   private static final SimpleWeightedRandomList<VillagerNpcRole> VISITOR_ROLES =
         new SimpleWeightedRandomList.Builder<VillagerNpcRole>().add(VillagerNpcRole.TRAVELLER, 0)
               .add(VillagerNpcRole.MIGRANT, 1)
               .add(VillagerNpcRole.SKILLED_PROFESSIONAL, 0)
               .add(VillagerNpcRole.MERCENARY, 0)
               .add(VillagerNpcRole.BEGGAR, 0)
               .add(VillagerNpcRole.SCOUNDREL, 0)
               .add(VillagerNpcRole.THIEF, 0)
               .add(VillagerNpcRole.MERCHANT, 0)
               .build();

   private void trySpawnVisitor(ServerLevel level) {

      List<VillagerInfo> visitors = BuildingUtil.getVisitors(getBuilding(), ServerVillagerStore.INSTANCE);
      if (visitors.size() >= MAX_VISITORS)
         return;

      Optional<BlockPos> spawnPos =
            getBuilding().getBounds().findRandomStandableSpot(level, level.getRandom(), SPAWN_SPOT_ATTEMPTS);
      if (spawnPos.isEmpty()) {
         LOGGER.debug("Couldn't find anywhere to spawn a visitor at Town Square {}", getBuilding().getBuildingId());
         return;
      }

      CivilizedVillager villager =
            EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, spawnPos.get(), EntitySpawnReason.EVENT);

      if (villager == null) {
         LOGGER.error(
               "Tried to spawn villager at a Town Square, but something went wrong during the entity spawning process.");
         return;
      }

      VillagerNpcRole visitorRole =
            VISITOR_ROLES.getRandomValue(villager.getRandom()).orElse(VillagerNpcRole.TRAVELLER);
      villager.getInfo().getNpcRoles().add(visitorRole);
      villager.getInfo().setHomeBuildingId(getBuilding().getBuildingId());
      villager.setDepartAt(chooseDepartureTime(level.getDayTime(), villager.getRandom()));
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);
      ServerBuildingsStore.INSTANCE.replicateChange(getBuilding(), StoreOperation.UPDATE);

      LOGGER.debug("Villager spawned at Town Square: {}", villager.getUUID());
   }

   private static long chooseDepartureTime(long dayTime, RandomSource random) {
      long startOfDay = dayTime - dayTime % 24000;
      return startOfDay + EARLIEST_DEPARTURE_TIME + random.nextInt(LATEST_DEPARTURE_TIME - EARLIEST_DEPARTURE_TIME);
   }
}
