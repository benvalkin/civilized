package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;

@Accessors(fluent = true)
@Getter
public class TakeToInventoryInstruction extends ConditionalHaulingInstruction<InventoryStockRequirement> {
   protected TakeToInventoryInstruction(
         List<InventoryStockRequirement> requirements,
         List<LoadedBuilding> sourceBuildings,
         String reservationKey) {
      super(requirements, sourceBuildings, reservationKey);
   }

   /**
    * Creates an instruction as long as the requirement can be met from the source buildings. When fulfilling this
    * instruction, the villager will take whatever it can get from each source building. items.
    */
   public static Optional<TakeToInventoryInstruction> createIfMetFromSourceBuildings(
         String reservationKey,
         InventoryStockRequirement requirement,
         List<LoadedBuilding> candidateSourceBuildings) {
      return createIfAnyMetFromSourceBuildings(reservationKey, List.of(requirement), candidateSourceBuildings);
   }

   /**
    * Creates an instruction as long as AT LEAST ONE of the requirements can be met from the source buildings. When
    * fulfilling this instruction, the villager will take whatever it can get from each source building - i.e. when one
    * requirement is satisfied and another isn't, the villager will still continue to other buildings in search of other
    * items.
    */
   public static Optional<TakeToInventoryInstruction> createIfAnyMetFromSourceBuildings(
         String reservationKey,
         List<InventoryStockRequirement> requirements,
         List<LoadedBuilding> candidateSourceBuildings) {

      boolean anyRequirementSatisfied =
            requirements.stream().anyMatch(r -> r.evaluate(reservationKey, candidateSourceBuildings).satisfied());
      if (!anyRequirementSatisfied)
         return Optional.empty();

      return Optional.of(new TakeToInventoryInstruction(requirements, candidateSourceBuildings, reservationKey));
   }

   /**
    * Creates an instruction only if ALL the requirements can be met from the source buildings. When fulfilling this
    * instruction, the villager will take whatever it can get from each source building - i.e. when one requirement is
    * satisfied and another isn't, the villager will still continue to other buildings in search of other items.
    */
   public static Optional<TakeToInventoryInstruction> createIfAllMetFromSourceBuildings(
         String reservationKey,
         List<InventoryStockRequirement> requirements,
         List<LoadedBuilding> sourceBuildings) {

      boolean allRequirementAvailable =
            requirements.stream().allMatch(r -> r.evaluate(reservationKey, sourceBuildings).satisfied());
      if (!allRequirementAvailable)
         return Optional.empty();

      return Optional.of(new TakeToInventoryInstruction(requirements, sourceBuildings, reservationKey));
   }

   @Override
   public HaulDecision takeItemsUntilSatisfied(
         CivilizedVillager villager,
         LoadedBuilding sourceBuilding,
         String reservationKey) {

      List<Container> chests = sourceBuilding.chests();

      List<ItemReservation> itemReservations = sourceBuilding.getReservationsExcluding(reservationKey);

      for (InventoryStockRequirement requirement : requirements) {
         int quota = outstandingAmount(villager, requirement);
         if (quota <= 0)
            continue; // villager already has enough of these items

         takeForRequirement(villager, requirement, chests, itemReservations, quota);
      }

      if (allRequirementsSatisfied(villager))
         return HaulDecision.REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO;

      return HaulDecision.REQUIREMENT_NOT_YET_SATISFIED;
   }

   @Override
   protected int outstandingAmount(CivilizedVillager villager, InventoryStockRequirement requirement) {

      if (requirement.disregardExistingCarriedStock()) {
         return requirement.idealAmount();
      }

      AggregateItemStack carried =
            ContainerHelper.countItems(requirement.getHaulInventory(villager), requirement.filter());
      return requirement.idealAmount() - carried.getCount();
   }

   @Override
   public String toString() {
      return super.toString() + ": ["
            + sourceBuildings.stream()
                  .map(b -> b.getBuilding().getBuildingType().toString())
                  .collect(Collectors.joining(", "))
            + "] -> "
            + requirements.stream().map(r -> r.inventoryType().toString()).distinct().collect(Collectors.joining(", "));
   }
}
