package com.uncreated.civilized.datagen;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.trading.MerchantTypes;

import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CivilizedMod.CIVILIZED_MOD_ID)
public class DataGen {

   @SubscribeEvent // on the mod event bus
   public static void gatherData(GatherDataEvent.Client event) {
      event.createProvider(CivilizedItemModelProvider::new);
      event.createProvider(CivilizedDataMapProvider::new);
      event.createDatapackRegistryObjects(
            new RegistrySetBuilder().add(MerchantTypes.REGISTRY_KEY, CivilizedMerchantTypeProvider::bootstrap));
   }
}
