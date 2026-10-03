package com.uncreated.civilized.core.villagerinfo.events;

import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
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

   // make sure this runs before other join handlers, e.g. the one adding the villager to LoadedVillagers, since they
   // need the
   // villager's id, which brand-new villagers only get here.
   // the priority here was changed to get villager#continuesExistingVillager to work.
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void entitySpawnFinalized(EntityJoinLevelEvent event) {

      if (event.getEntity() instanceof CivilizedVillager villager) {

         if (!villager.level().isClientSide) {
            if (!event.loadedFromDisk() && !villager.continuesExistingVillager()) {
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

      ServerVillagerStore.INSTANCE.removeVillager(villager.getInfo(), true);
   }
}
