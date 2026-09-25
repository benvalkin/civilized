package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import java.util.Comparator;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
@Getter
public class InventoryStockRequirement extends ItemStockRequirement {

   private final VillagerInventoryType inventoryType;

   public InventoryStockRequirement(
         String key,
         Predicate<ItemStack> test,
         Comparator<ItemStack> preference,
         int minimumAcceptableAmount,
         int idealAmount,
         VillagerInventoryType inventoryType) {
      super(key, test, preference, minimumAcceptableAmount, idealAmount);
      this.inventoryType = inventoryType;
   }

   public InventoryStockRequirement(
         String key,
         Predicate<ItemStack> test,
         int minimumAcceptableAmount,
         int idealAmount,
         VillagerInventoryType inventoryType) {
      this(key, test, ContainerHelper.NO_PREFERENCE, minimumAcceptableAmount, idealAmount, inventoryType);
   }

   public StockResult evaluate(CivilizedVillager villager) {
      Container inventory = getHaulInventory(villager);
      AggregateItemStack stock = ContainerHelper.countItems(inventory, filter);
      boolean satisfied = stock.getCount() >= minimumAcceptableAmount;
      return new StockResult(satisfied, stock);
   }

   @Override
   public @NotNull Container getHaulInventory(CivilizedVillager villager) {
      return villager.getInventory(inventoryType);
   }

   public record StockResult(boolean satisfied, AggregateItemStack stock) {
   }
}
