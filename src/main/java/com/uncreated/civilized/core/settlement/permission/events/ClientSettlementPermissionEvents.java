package com.uncreated.civilized.core.settlement.permission.events;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.settlement.permission.ClientSettlementPermissionStore;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID, value = Dist.CLIENT)
public class ClientSettlementPermissionEvents {

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      ClientSettlementPermissionStore.resetClient();
   }
}
