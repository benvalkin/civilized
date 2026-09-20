package com.uncreated.civilized.core.building.logistics.hauling.requirement;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Accessors(fluent = true)
@Getter
public abstract class ItemStockRequirement {
   private final String key;
   protected final Predicate<ItemStack> filter;
   protected final int minimumAmountToSatisfy;
   protected final int idealAmount;

   public static final int UNLIMITED = Integer.MAX_VALUE;

   public ItemStockRequirement(String key, Predicate<ItemStack> filter, int minimumAmountToSatisfy, int idealAmount) {
      this.key = key;
      this.filter = filter;
      this.minimumAmountToSatisfy = minimumAmountToSatisfy;
      this.idealAmount = idealAmount;

      if (filter.test(ItemStack.EMPTY))
         throw new IllegalArgumentException("ItemStockRequirement filter is not allowed to match empty items.");

      if (minimumAmountToSatisfy <= 0 || idealAmount <= 0 || minimumAmountToSatisfy > idealAmount)
         throw new IllegalArgumentException(
               "Invalid ItemStockRequirement amounts - at least one was 0, or min was greater than max");
   }

   public boolean insatiable() {
      return idealAmount == UNLIMITED;
   }

   /**
    * Which inventory the villager will use while transporting items/picking up tools.
    */
   public abstract @NotNull Container getHaulInventory(CivilizedVillager villager);

   public StockResult evaluate(List<LoadedBuilding> candidateSourceBuildings) {

      AggregateItemStack grossStock = new AggregateItemStack();
      Map<LoadedBuilding, AggregateItemStack> satisfiedBuildings = new HashMap<>();
      for (LoadedBuilding candidateBuilding : candidateSourceBuildings) {
         List<Container> chests = candidateBuilding.findChests();
         AggregateItemStack buildingStock =
               calculateBuildingStock(chests, candidateBuilding.getItemReservations().values());

         if (buildingStock.getCount() > 0) {
            grossStock.add(buildingStock);
            satisfiedBuildings.put(candidateBuilding, buildingStock);
         }
      }

      boolean satisfied = grossStock.getCount() >= minimumAmountToSatisfy;
      return new BuildingStockRequirement.StockResult(satisfied, grossStock, satisfiedBuildings);
   }

   public AggregateItemStack calculateBuildingStock(
         List<Container> chests,
         Collection<ItemReservation> reservations) {
      AggregateItemStack buildingStock = new AggregateItemStack();
      for (Container chest : chests) {
         AggregateItemStack chestStock = ContainerHelper.countItems(chest, filter);
         buildingStock.add(chestStock);
      }

      for (ItemReservation reservationEntry : reservations) {
         buildingStock.removeMatching(reservationEntry.filter(), reservationEntry.amount());
      }
      return buildingStock;
   }

   public record StockResult(boolean satisfied, AggregateItemStack totalStock,
         Map<LoadedBuilding, AggregateItemStack> perBuildingStock) {
   }

   @Override
   public String toString() {
      if (minimumAmountToSatisfy == idealAmount)
         return key + ": " + minimumAmountToSatisfy;

      return key + ": " + minimumAmountToSatisfy + "-" + idealAmount;
   }
}
