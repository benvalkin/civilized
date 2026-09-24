package com.uncreated.civilized.core.building.logistics;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;

@Getter
public class AggregateItemStack {
   private final List<ItemStack> itemStacks;
   private final boolean mutable;
   private int count;

   public AggregateItemStack(List<ItemStack> itemStacks, boolean mutable) {
      this.mutable = mutable;
      this.itemStacks = itemStacks.stream().map(i -> mutable ? i : i.copy()).collect(Collectors.toList());
      this.count = itemStacks.stream().mapToInt(ItemStack::getCount).sum();
   }

   public AggregateItemStack(List<ItemStack> itemStacks) {
      this(itemStacks, false);
   }

   public AggregateItemStack(boolean mutable) {
      this.mutable = mutable;
      this.itemStacks = Lists.newArrayList();
      this.count = 0;
   }

   public AggregateItemStack() {
      this(false);
   }

   public AggregateItemStack(boolean mutable, AggregateItemStack... aggregateItemStacks) {
      this.mutable = mutable;
      this.itemStacks = Lists.newArrayList();
      for (AggregateItemStack other : aggregateItemStacks)
         for (ItemStack itemStack : other.itemStacks)
            this.itemStacks.add(mutable ? itemStack : itemStack.copy());
      this.count = itemStacks.stream().mapToInt(ItemStack::getCount).sum();
   }

   public AggregateItemStack(AggregateItemStack... aggregateItemStacks) {
      this(false, aggregateItemStacks);
   }

   public void add(ItemStack itemStack) {
      this.itemStacks.add(mutable ? itemStack : itemStack.copy());
      this.count += itemStack.getCount();
   }

   public void add(AggregateItemStack other) {
      for (ItemStack itemStack : other.itemStacks)
         this.itemStacks.add(mutable ? itemStack : itemStack.copy());

      this.count += other.getCount();
   }

   public void removeMatching(Predicate<ItemStack> predicate, int upTo) {
      for (ItemStack itemStack : itemStacks) {
         if (predicate.test(itemStack)) {
            int toRemove = Math.min(upTo, itemStack.getCount());
            itemStack.shrink(toRemove);
            count -= toRemove;
            upTo -= toRemove;
            if (upTo <= 0)
               break;
         }
      }
      itemStacks.removeIf(ItemStack::isEmpty);
   }

   public boolean hasItems() {
      return count > 0;
   }

   @Override
   public String toString() {
      return count + " [" + itemStacks.stream().map(s -> s.getItem().toString()).collect(Collectors.joining(", "))
            + "]";
   }
}
