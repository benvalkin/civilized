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

   /**
    * Calculates the items in {@code storage} that this reservation holds on to. Each item is only
    * counted once, i.e. two entries matching the same items do not hold onto the same item stack.
    */
   public AggregateItemStack calculateReservedItems(List<Container> storage) {
      AggregateItemStack reservedItems = new AggregateItemStack();

      // how many items in each slot have not been claimed by an earlier entry yet
      Map<Container, int[]> unclaimed = new IdentityHashMap<>();
      for (Container container : storage) {
         int[] counts = new int[container.getContainerSize()];
         for (int i = 0; i < counts.length; i++)
            counts[i] = container.getItem(i).getCount();
         unclaimed.put(container, counts);
      }

      for (Entry entry : entries) {
         int quota = entry.amount();

         for (Container container : storage) {
            int[] counts = unclaimed.get(container);

            for (int i = 0; i < counts.length && quota > 0; i++) {
               ItemStack item = container.getItem(i);
               if (counts[i] <= 0 || !entry.filter().test(item))
                  continue;

               int claimed = Math.min(counts[i], quota);
               counts[i] -= claimed;
               quota -= claimed;
               reservedItems.add(item.copyWithCount(claimed));
            }

            if (quota <= 0)
               break;
         }
      }

      return reservedItems;
   }

   @Override
   public String toString() {
      String amounts = entries.stream().map(entry -> String.valueOf(entry.amount())).collect(Collectors.joining(", "));
      return key.name() + ":[" + amounts + "] (" + key.party() + ")";
   }
}
