package com.uncreated.civilized.core.settlement.events;

import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class SettlementStoreEvents {

   @SubscribeEvent
   public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
         ServerSettlementsStore.INSTANCE.replicateFullToNewClient(serverPlayer);
         ServerSettlementPermissionStore.INSTANCE.replicateFullToNewClient(serverPlayer);
      }
   }
}
