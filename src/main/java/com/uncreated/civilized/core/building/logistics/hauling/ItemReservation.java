package com.uncreated.civilized.core.building.logistics.hauling;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;


@Accessors(fluent = true)
@Getter
public class ItemReservation {
   private final ReservationKey key;
   private final List<Entry> entries;


   public record Entry(Predicate<ItemStack> filter, int amount) {
      public Entry {
         if (filter.test(ItemStack.EMPTY))
            throw new IllegalArgumentException("ItemReservation filter is not allowed to match empty items.");

         if (amount <= 0)
            throw new IllegalArgumentException("ItemReservation amount must be greater than zero");
      }
   }

   public ItemReservation(ReservationKey key, List<Entry> entries) {
      if (entries.isEmpty())
         throw new IllegalArgumentException("ItemReservation must reserve at least one kind of item");

      this.key = key;
      this.entries = List.copyOf(entries);
   }

   @Override
   public String toString() {
      String amounts = entries.stream().map(entry -> String.valueOf(entry.amount())).collect(Collectors.joining(", "));
      return key.name() + ":[" + amounts + "] (" + key.party() + ")";
   }
}
