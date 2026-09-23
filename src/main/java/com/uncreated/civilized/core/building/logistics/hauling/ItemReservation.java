package com.uncreated.civilized.core.building.logistics.hauling;

import java.util.List;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
@Getter
public class ItemReservation {
   private final String key;
   private final String name;
   protected final Predicate<ItemStack> filter;
   protected final int amount;

   public ItemReservation(String key, String name, Predicate<ItemStack> filter, int amount) {
      this.key = key;
      this.name = name;
      this.filter = filter;
      this.amount = amount;

      if (filter.test(ItemStack.EMPTY))
         throw new IllegalArgumentException("ItemReservation filter is not allowed to match empty items.");

      if (this.amount <= 0)
         throw new IllegalArgumentException("ItemReservation amount must be greater than zero");
   }

   public AggregateItemStack calculateReservedItems(List<Container> storage) {

      int quota = amount;
      AggregateItemStack reservedItems = new AggregateItemStack();
      for (Container container : storage) {
         for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack item = container.getItem(i);
            if (!filter.test(item))
               continue;

            // add until we reach the reservation's amount
            ItemStack toAdd = item.copy();
            // clamp the toAdd item amount to the remaining quota in case this stack is bigger than what we have left to
            // add
            toAdd.setCount(Math.min(toAdd.getCount(), quota));
            quota -= toAdd.getCount();
            reservedItems.add(toAdd);

            if (quota <= 0)
               break;
         }
      }

      return reservedItems;
   }

   @Override
   public String toString() {
      return name + " (" + key + "): " + amount;
   }
}
