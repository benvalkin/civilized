package com.uncreated.civilized.neoforge.registration;

import com.uncreated.civilized.core.trading.MerchantType;
import com.uncreated.civilized.core.trading.MerchantTypes;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public class DatapackRegistries {

   @SubscribeEvent // on the mod event bus
   public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
      // not synced, since only the server needs it. Trades reach clients through the trading menu
      event.dataPackRegistry(MerchantTypes.REGISTRY_KEY, MerchantType.CODEC);
   }
}
