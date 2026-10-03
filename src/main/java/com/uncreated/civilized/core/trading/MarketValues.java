package com.uncreated.civilized.core.trading;

import java.util.Map;
import java.util.Optional;

import com.uncreated.civilized.neoforge.registration.DataMapRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Looks up what items are worth on the market. Values come from the {@code civilized:market_values} data map, so
 * datapacks can change them.
 * <p>
 * This Neoforge data map here may be swapped out with another backend to support other modloaders in the future.
 */
public class MarketValues {

   /** @return the item's market value, or nothing if it can't be traded */
   public static Optional<MarketValue> get(ItemStack stack) {
      if (stack.isEmpty())
         return Optional.empty();

      return Optional.ofNullable(stack.getItemHolder().getData(DataMapRegistry.MARKET_VALUE));
   }

   /** Every item that has a market value. Entries for tags have already been expanded into their items. */
   public static Map<ResourceKey<Item>, MarketValue> all() {
      return BuiltInRegistries.ITEM.getDataMap(DataMapRegistry.MARKET_VALUE);
   }

   public static boolean isTradeable(ItemStack stack) {
      return get(stack).isPresent();
   }
}
