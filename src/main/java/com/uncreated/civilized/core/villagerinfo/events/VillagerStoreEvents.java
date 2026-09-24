package com.uncreated.civilized.core.villagerinfo.events;

import java.util.Optional;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class VillagerStoreEvents {

   @SubscribeEvent
   public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer)
         ServerVillagerStore.INSTANCE.replicateFullToNewClient(serverPlayer);
      else
         ClientVillagerStore.loadClient();
   }

   @SubscribeEvent
   public static void entitySpawnFinalized(EntityJoinLevelEvent event) {

      if (event.getEntity() instanceof CivilizedVillager villager) {

         if (!villager.level().isClientSide) {
            if (!event.loadedFromDisk()) {
               villager.initBrandNewVillager();
            } else {
               villager.initVillagerFromSave();
            }
            villager.serverFinalizeSpawn();
         }
      }
   }

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.getEntity().level().isClientSide())
         return;

      if (!(event.getEntity() instanceof CivilizedVillager villager))
         return;

      if (!event.getEntity().level().isClientSide()) {
         ServerVillagerStore.INSTANCE.delete(villager).ifPresent(v -> v.setDeceased(true));
         ServerVillagerStore.INSTANCE.setDirty();
         ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.DELETE);

         Optional<Building> home = ServerBuildingsStore.INSTANCE.find(villager.getInfo().getHomeBuildingId());
         home.ifPresent(h -> h.getOccupantIds().remove(villager.getInfo().getHomeBuildingId()));

         Optional<Building> worksite = ServerBuildingsStore.INSTANCE.find(villager.getInfo().getHomeBuildingId());
         worksite.ifPresent(w -> w.getOccupantIds().remove(villager.getInfo().getPrimaryWorksiteId()));

         ServerBuildingsStore.INSTANCE.find(villager.getInfo().getHomeBuildingId()).ifPresent(b -> {
            ServerBuildingsStore.INSTANCE.replicateChange(b, StoreOperation.UPDATE);
         });
      }
   }
}
