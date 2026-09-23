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

   private static final SimpleWeightedRandomList<VillagerNpcRole> VISITOR_ROLES =
         new SimpleWeightedRandomList.Builder<VillagerNpcRole>().add(VillagerNpcRole.MIGRANT, 20)
               .add(VillagerNpcRole.TRAVELLER, 1)
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
      villager.getInfo().setHomeBuildingId(getBuilding().getBuildingId());
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);

      LOGGER.debug("Villager spawned at Inn: {}", villager.getUUID());
   }
}
