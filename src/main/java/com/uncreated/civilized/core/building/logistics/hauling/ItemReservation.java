package com.uncreated.civilized.core.building.logistics.hauling;

import java.util.List;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
@Getter
public class ItemReservation {
   private final String key;
   protected final Predicate<ItemStack> filter;
   protected final int amount;

   public ItemReservation(String key, Predicate<ItemStack> filter, int amount) {
      this.key = key;
      this.filter = filter;
      this.amount = amount;

      if (filter.test(ItemStack.EMPTY))
         throw new IllegalArgumentException("ItemReservation filter is not allowed to match empty items.");

      if (this.amount <= 0)
         throw new IllegalArgumentException("ItemReservation amount must be greater than zero");
   }

   public AggregateItemStack calculateReservedItems(List<Container> storage) {
      AggregateItemStack reservedStock = new AggregateItemStack();
      for (Container container : storage) {
         reservedStock.add(ContainerHelper.countItems(container, filter));
      }
      return reservedStock;
   }

   @Override
   public String toString() {
      return key + ": " + amount;
   }
}
