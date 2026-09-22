package com.uncreated.civilized.core.building.production.bills;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
@Getter
public class ItemFilter {
   private final ItemFilterMode mode;
   private final List<ItemStack> items;

   public ItemFilter(ItemFilterMode mode, List<ItemStack> items) {
      this.mode = mode;
      this.items = items;
   }

   public boolean isAllowed(ItemStack stack) {
      if (items.isEmpty()) // everything is allowed unless the filter specifies an item
         return true;

      return switch (mode) {
      case WHITELIST -> items.stream().anyMatch(i -> ItemStack.isSameItem(i, stack));
      case BLACKLIST -> items.stream().noneMatch(i -> ItemStack.isSameItem(i, stack));
      };
   }

   public static ItemFilter allowAllItems() {
      return new ItemFilter(ItemFilterMode.WHITELIST, new LinkedList<>());
   }
}
