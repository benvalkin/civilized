package com.uncreated.civilized.core.building.logistics.orders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;

import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

@Getter
public abstract class LogisticsOrder {

   protected final Level level;
   protected final String key;
   protected final Predicate<ItemStack> itemSearch;
   protected final Origin origin;
   private long startTime;
   private long expiryTicks;

   public LogisticsOrder(Level level, String key, Predicate<ItemStack> itemSearch, Origin origin) {
      this.level = level;
      this.key = key;
      this.itemSearch = itemSearch;
      this.origin = origin;
      this.startTime = -1;
      this.expiryTicks = -1;
   }

   public void setExpiry(long ticksToExpire) {
      this.startTime = level.getGameTime();
      this.expiryTicks = ticksToExpire;
   }

   public boolean isExpired() {
      if (expiryTicks == -1 || startTime == -1)
         return false;

      return level.getGameTime() - startTime >= expiryTicks;
   }

   public static List<Container> findChests(Level level, Building... buildings) {

      List<Container> chests = new ArrayList<>();
      for (Building building : buildings) {
         chests.addAll(
               building.getBounds()
                     .getBlockEntitiesInsideBuilding(level, true)
                     .stream()
                     .filter(e -> e instanceof ChestBlockEntity)
                     .map(e -> ((Container) e))
                     .toList());
      }

      return chests;
   }

   protected AggregateItemStack calculateStock(Collection<Container> containers) {
      AggregateItemStack stock = new AggregateItemStack();
      containers.forEach(c -> {
         for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack itemStack = c.getItem(i);
            if (itemStack.isEmpty())
               continue;

            if (!itemSearch.test(itemStack))
               continue;

            stock.add(itemStack);
         }
      });

      return stock;
   }

   public enum Origin {
      AUTOMATIC, PLAYER_CREATED
   }
}
