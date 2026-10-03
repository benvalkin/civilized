package com.uncreated.civilized.neoforge.registration;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.core.trading.MarketValue;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public class DataMapRegistry {

   /** Loaded from {@code data/<namespace>/data_maps/item/market_values.json}. */
   public static final DataMapType<Item, MarketValue> MARKET_VALUE =
         DataMapType
               .builder(
                     ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "market_values"),
                     Registries.ITEM,
                     MarketValue.CODEC)
               // synced so the client can show values, e.g. in tooltips
               .synced(MarketValue.CODEC, false)
               .build();

   @SubscribeEvent // on the mod event bus
   public static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
      event.register(MARKET_VALUE);
   }
}
