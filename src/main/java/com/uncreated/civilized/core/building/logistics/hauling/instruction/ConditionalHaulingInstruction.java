package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;
import java.util.stream.Collectors;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;

@Accessors(fluent = true)
@Getter
public abstract class ConditionalHaulingInstruction<T extends ItemStockRequirement> implements IHaulingInstruction {
   protected final List<T> requirements;
   protected final List<LoadedBuilding> sourceBuildings;
   protected int currentBuildingIndex;
   protected final ReservationKey reservationKey;

   protected ConditionalHaulingInstruction(
         List<T> requirements,
         List<LoadedBuilding> sourceBuildings,
         ReservationKey reservationKey) {
      this.requirements = requirements;
      this.sourceBuildings = sourceBuildings;
      this.reservationKey = reservationKey;
      this.currentBuildingIndex = 0;

      if (requirements.isEmpty()) {
         throw new IllegalArgumentException("requirements cannot be empty");
      }

      if (sourceBuildings.isEmpty()) {
         throw new IllegalArgumentException("sourceBuildings cannot be empty");
      }
   }

   public abstract HaulDecision takeItemsUntilSatisfied(CivilizedVillager villager, LoadedBuilding sourceBuilding);

   /**
    * How many more items of this requirement the villager still wants to pick up. 0 once it needs no more.
    */
   protected abstract int outstandingAmount(CivilizedVillager villager, T requirement);

   protected boolean allRequirementsSatisfied(CivilizedVillager villager) {
      return requirements.stream().allMatch(r -> outstandingAmount(villager, r) <= 0);
   }

   /**
    * Whether the building holds anything for a requirement that still needs items, i.e. whether it is worth the
    * villager walking over there.
    */
   public boolean hasUsefulStock(CivilizedVillager villager, LoadedBuilding building) {
      List<Container> chests = building.chests();

      for (T requirement : requirements) {
         if (outstandingAmount(villager, requirement) <= 0)
            continue;

         if (requirement.calculateBuildingStock(chests, building.getReservationsExcluding(reservationKey.party()))
               .hasItems())
            return true;
      }

      return false;
   }

   /** Everything the villager is carrying for these requirements. */
   public AggregateItemStack countCarriedItems(CivilizedVillager villager) {
      AggregateItemStack carried = new AggregateItemStack();
      for (T requirement : requirements)
         carried.add(ContainerHelper.countItems(requirement.getHaulInventory(villager), requirement.filter()));

      return carried;
   }

   /**
    * Takes up to {@code quota} items for a single requirement out of the given chests, and returns how many were
    * actually taken.
    */
   protected int takeForRequirement(
         CivilizedVillager villager,
         T requirement,
         List<Container> sourceChests,
         List<ItemReservation> itemReservations,
         int quota) {

      quota = adjustQuotaForReservedItems(quota, itemReservations, requirement, sourceChests);

      Container haulInventory = requirement.getHaulInventory(villager);

      return ContainerHelper
            .transferNicely(sourceChests, haulInventory, requirement.filter(), requirement.preference(), quota);
   }

   protected static int adjustQuotaForReservedItems(
         int quota,
         List<ItemReservation> itemReservations,
         ItemStockRequirement requirement,
         List<Container> chests) {
      int unreserved = requirement.calculateBuildingStock(chests, itemReservations).getCount();
      return Math.min(quota, unreserved);
   }

   @Override
   public String toString() {
      return requirements.stream().map(ItemStockRequirement::toString).collect(Collectors.joining(", "));
   }

   public enum HaulDecision {
      REQUIREMENT_NOT_YET_SATISFIED, REQUIREMENT_SATISFIED_OFFLOAD_ITEMS, REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO
   }
}
