package com.uncreated.civilized.util;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ContainerHelper {
   public static ItemStack addItemNicely(Container container, ItemStack stack) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack itemstack = stack.copy();
         moveItemToOccupiedSlotsWithSameType(container, itemstack);
         if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            moveItemToEmptySlots(container, itemstack);
            return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
         }
      }
   }

   public static ItemStack addItemNicely(List<Container> containers, ItemStack stack) {
      for (Container container : containers) {
         stack = addItemNicely(container, stack);
         if (stack.isEmpty())
            break;
      }
      return stack;
   }

   private static void moveItemToEmptySlots(Container container, ItemStack stack) {
      for (int i = 0; i < container.getContainerSize(); ++i) {
         ItemStack itemstack = container.getItem(i);
         if (itemstack.isEmpty()) {
            container.setItem(i, stack.copyAndClear());
            container.setChanged();
            return;
         }
      }

   }

   private static void moveItemToOccupiedSlotsWithSameType(Container container, ItemStack stack) {
      for (int i = 0; i < container.getContainerSize(); ++i) {
         ItemStack itemstack = container.getItem(i);
         if (ItemStack.isSameItemSameComponents(itemstack, stack)) {
            moveItemsBetweenStacks(container, stack, itemstack);
            if (stack.isEmpty()) {
               return;
            }
         }
      }

   }

   private static void moveItemsBetweenStacks(Container container, ItemStack stack, ItemStack other) {
      int i = container.getMaxStackSize(other);
      int j = Math.min(stack.getCount(), i - other.getCount());
      if (j > 0) {
         other.grow(j);
         stack.shrink(j);
         container.setChanged();
      }
   }

   @Deprecated
   public static int transferNicely(
         Container fromContainer,
         Container toContainer,
         Predicate<ItemStack> searchFunction,
         int upTo) {
      return transferNicely(fromContainer, toContainer, searchFunction, NO_PREFERENCE, upTo);
   }

   public static final Comparator<ItemStack> NO_PREFERENCE = (item1, item2) -> 0;

   public static int transferNicely(
         Container fromContainer,
         Container toContainer,
         Predicate<ItemStack> searchFunction,
         Comparator<ItemStack> preferenceFunction,
         int upTo) {
      return transferNicely(List.of(fromContainer), toContainer, searchFunction, preferenceFunction, upTo);
   }

   public static int transferNicely(
         List<Container> fromContainers,
         Container toContainer,
         Predicate<ItemStack> searchFunction,
         Comparator<ItemStack> preferenceFunction,
         int upTo) {

      if (upTo <= 0)
         return 0;

      List<ContainerItemReference> items = new LinkedList<>();
      for (int c = 0; c < fromContainers.size(); c++) {
         Container container = fromContainers.get(c);
         for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack item = container.getItem(i);
            if (!searchFunction.test(item))
               continue;

            items.add(new ContainerItemReference(item, c, i));
         }
      }

      if (preferenceFunction != NO_PREFERENCE)
         items.sort((i1, i2) -> preferenceFunction.compare(i1.item(), i2.item()));

      int addedSoFar = 0;

      for (ContainerItemReference containerItem : items) {
         ItemStack toAdd = containerItem.item().copy();
         if (addedSoFar + toAdd.getCount() > upTo)
            toAdd.shrink(addedSoFar + toAdd.getCount() - upTo);

         ItemStack remainder = addItemNicely(toContainer, toAdd);
         ItemStack actuallyAdded = toAdd.copyWithCount(toAdd.getCount() - remainder.getCount());
         addedSoFar += actuallyAdded.getCount();

         containerItem.item().shrink(actuallyAdded.getCount());
         int container = containerItem.container();
         int slot = containerItem.slot();
         fromContainers.get(container).setItem(slot, containerItem.item());

         if (addedSoFar >= upTo)
            break;
      }

      return addedSoFar;
   }

   public static Optional<ItemSearchResult> findItem(Container container, Predicate<ItemStack> itemSearch) {

      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack item = container.getItem(i);
         if (itemSearch.test(item))
            return Optional.of(new ItemSearchResult(item, i));
      }

      return Optional.empty();
   }

   public static AggregateItemStack countItems(Container container, Predicate<ItemStack> itemSearch) {

      AggregateItemStack result = new AggregateItemStack();

      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack item = container.getItem(i);
         if (itemSearch.test(item))
            result.add(item);
      }

      return result;
   }

   public record ItemSearchResult(ItemStack itemStack, int slot) {
   }
}
