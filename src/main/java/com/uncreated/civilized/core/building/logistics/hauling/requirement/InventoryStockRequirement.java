package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
@Getter
public class InventoryStockRequirement extends ItemStockRequirement {

   private final Function<CivilizedVillager, Container> getInventory;

   public InventoryStockRequirement(
         String key,
         Predicate<ItemStack> test,
         Comparator<ItemStack> preference,
         int minimumAcceptableAmount,
         int idealAmount,
         Function<CivilizedVillager, Container> getInventory) {
      super(key, test, preference, minimumAcceptableAmount, idealAmount);
      this.getInventory = getInventory;
   }

   public InventoryStockRequirement(
         String key,
         Predicate<ItemStack> test,
         int minimumAcceptableAmount,
         int idealAmount,
         Function<CivilizedVillager, Container> getInventory) {
      this(key, test, ContainerHelper.NO_PREFERENCE, minimumAcceptableAmount, idealAmount, getInventory);
   }

   public StockResult evaluate(CivilizedVillager villager) {
      Container inventory = getHaulInventory(villager);
      AggregateItemStack stock = ContainerHelper.countItems(inventory, filter);
      boolean satisfied = stock.getCount() >= minimumAcceptableAmount;
      return new StockResult(satisfied, stock);
   }

   @Override
   public @NotNull Container getHaulInventory(CivilizedVillager villager) {
      return getInventory.apply(villager);
   }

   public record StockResult(boolean satisfied, AggregateItemStack stock) {
   }

   public static List<InventoryStockRequirement> cast(List<EquipmentRequirement> requirements) {
      return requirements.stream().map(e -> (InventoryStockRequirement) e).toList();
   }
}
