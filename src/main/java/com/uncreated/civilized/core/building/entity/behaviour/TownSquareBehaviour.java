package com.uncreated.civilized.core.building.entity.behaviour;

import java.util.List;

import com.uncreated.civilized.core.StoreOperation;
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
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntitySpawnReason;

public class TownSquareBehaviour extends BuildingBehaviour {

   private final DailyEventScheduler eventScheduler;

   public TownSquareBehaviour(LoadedBuilding entity) {
      super(entity);
      eventScheduler = new DailyEventScheduler(5, 0, 6000, 0.1f);
   }

   @Override
   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      eventScheduler.tick(level, gameTime, () -> trySpawnVisitor(level));
      tryDespawnVisitors(level, dayTime);
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

      List<VillagerInfo> occupants = BuildingUtil.getResidents(getBuilding(), ServerVillagerStore.INSTANCE);
      if (occupants.size() >= 4)
         return;

      BlockPos insidePos = getBuilding().getBounds().findRandomInsideFloorBlock(level);

      CivilizedVillager villager =
            EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, insidePos, EntitySpawnReason.EVENT);

      if (villager == null) {
         LOGGER.error("Tried to spawn villager at a Inn, but something went wrong during the entity spawning process.");
         return;
      }

      VillagerNpcRole visitorRole =
            VISITOR_ROLES.getRandomValue(villager.getRandom()).orElse(VillagerNpcRole.TRAVELLER);
      villager.getInfo().getNpcRoles().add(visitorRole);
      // // visitors "occupy" the town square until further notice
      // getBuilding().getOccupantIds().add(villager.getVillagerId());
      // villager.getInfo().setHomeBuildingId(getBuilding().getBuildingId());
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);

      LOGGER.debug("Villager spawned at Inn: {}", villager.getUUID());
   }

   private void tryDespawnVisitors(ServerLevel level, long dayTime) {

      long todayTime = dayTime % 24000;
      boolean onTheHour = todayTime % 1000 == 0;

      if (onTheHour) {
         if (todayTime >= 11000 && todayTime < 16000) { // between between 5-10pm
            // todo: every visitor spawned by this town square has a 20% chance of despawning
         } else {
            // todo: despawn all visitors that remain spawned by this town square
         }
      }
   }
}
