package com.uncreated.civilized.core.building.logistics.orders.task;

import java.util.Collection;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.orders.LogisticsOrder;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

@Getter
public abstract class TaskItemRequirement extends LogisticsOrder {

   private final String taskName;

   public TaskItemRequirement(Level level, String taskName, Predicate<ItemStack> itemSearch, Origin origin) {
      super(level, taskName, itemSearch, origin);
      this.taskName = taskName;
   }

   protected abstract boolean isStockSufficient(AggregateItemStack sourceStock);

   protected abstract boolean villagerHasRequiredItems(Container villagerInventory);

   protected abstract int getItemCountRequiredForTask(AggregateItemStack sourceStock);

   public PendingRequiredItems getRequiredItemsToTake(Building source, CivilizedVillager villager, Level level) {
      Collection<Container> sourceChests =
            source.getBounds()
                  .getBlockEntitiesInsideBuilding(level, true)
                  .stream()
                  .filter(e -> e instanceof ChestBlockEntity)
                  .map(e -> ((Container) e))
                  .toList();
      AggregateItemStack sourceStock = calculateStock(sourceChests);

      PendingRequiredItems.StockInfo stockInfo = new PendingRequiredItems.StockInfo(sourceChests, sourceStock);

      int requiredAmountToTake = getItemCountRequiredForTask(sourceStock);
      boolean isStockSufficient = isStockSufficient(sourceStock);
      boolean willTake = isStockSufficient && requiredAmountToTake > 0;
      boolean villagerHasRequiredItems = villagerHasRequiredItems(villager.getWorkInputInventory());

      return new PendingRequiredItems(
            itemSearch,
            requiredAmountToTake,
            isStockSufficient,
            willTake,
            villagerHasRequiredItems,
            stockInfo);
   }

   public boolean takeRequiredItems(CivilizedVillager villager, PendingRequiredItems pendingRequiredItems) {

      int quota = pendingRequiredItems.requiredAmountToTake();

      for (Container source : pendingRequiredItems.stock().getSourceChests()) {
         int transferred = ContainerHelper.transferNicely(source, villager.getWorkInputInventory(), itemSearch, quota);

         quota -= transferred;

         if (quota <= 0)
            break;
      }

      return quota < pendingRequiredItems.requiredAmountToTake();
   }

   public boolean returnItems(CivilizedVillager villager, PendingRequiredItems pendingRequiredItems) {

      int quota = pendingRequiredItems.requiredAmountToTake();

      for (Container source : pendingRequiredItems.stock().getSourceChests()) {
         int transferred = ContainerHelper.transferNicely(villager.getLogisticsInventory(), source, itemSearch, quota);

         quota -= transferred;

         if (quota <= 0)
            break;
      }

      return quota < pendingRequiredItems.requiredAmountToTake();
   }
}
