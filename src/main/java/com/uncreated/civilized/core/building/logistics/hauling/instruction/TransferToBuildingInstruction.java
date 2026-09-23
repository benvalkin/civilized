package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.BuildingStockRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;

@Accessors(fluent = true)
@Getter
public class TransferToBuildingInstruction extends ConditionalHaulingInstruction<BuildingStockRequirement> {
   private final LoadedBuilding destinationBuilding;

   protected TransferToBuildingInstruction(
         List<BuildingStockRequirement> requirements,
         List<LoadedBuilding> sourceBuildings,
         LoadedBuilding candidateSourceBuildings,
         String reservationParty,
         String reservationName) {
      super(requirements, sourceBuildings, reservationParty, reservationName);
      this.destinationBuilding = candidateSourceBuildings;
   }

   /**
    * Creates an instruction as long as the requirement can be met from the source buildings. When fulfilling this
    * instruction, the villager will take whatever it can get from each source building.
    */
   public static Optional<TransferToBuildingInstruction> createIfMetFromSourceBuildings(
         String reservationParty,
         String reservationName,
         BuildingStockRequirement requirement,
         LoadedBuilding destinationBuilding,
         List<LoadedBuilding> candidateSourceBuildings) {
      return createIfAnyMetFromSourceBuildings(
            reservationParty,
            reservationName,
            List.of(requirement),
            destinationBuilding,
            candidateSourceBuildings);
   }

   /**
    * Creates an instruction as long as AT LEAST ONE of the requirements can be met from the source buildings. When
    * fulfilling this instruction, the villager will take whatever it can get from each source building - i.e. when one
    * requirement is satisfied and another isn't, the villager will still continue to other buildings in search of other
    * items.
    */
   public static Optional<TransferToBuildingInstruction> createIfAnyMetFromSourceBuildings(
         String reservationParty,
         String reservationName,
         List<BuildingStockRequirement> requirements,
         LoadedBuilding destinationBuilding,
         List<LoadedBuilding> sourceBuildings) {

      boolean anyRequirementAvailable =
            requirements.stream().anyMatch(r -> r.evaluate(reservationParty, sourceBuildings).satisfied());
      if (!anyRequirementAvailable)
         return Optional.empty();

      return Optional.of(
            new TransferToBuildingInstruction(
                  requirements,
                  sourceBuildings,
                  destinationBuilding,
                  reservationParty,
                  reservationName));
   }

   /**
    * Creates an instruction only if ALL the requirements can be met from the source buildings. When fulfilling this
    * instruction, the villager will take whatever it can get from each source building - i.e. when one requirement is
    * satisfied and another isn't, the villager will still continue to other buildings in search of other items.
    */
   public static Optional<TransferToBuildingInstruction> createIfAllMetFromSourceBuildings(
         String reservationParty,
         String reservationName,
         List<BuildingStockRequirement> requirements,
         LoadedBuilding destinationBuilding,
         List<LoadedBuilding> sourceBuildings) {

      boolean allRequirementAvailable =
            requirements.stream().allMatch(r -> r.evaluate(reservationParty, sourceBuildings).satisfied());
      if (!allRequirementAvailable)
         return Optional.empty();

      return Optional.of(
            new TransferToBuildingInstruction(
                  requirements,
                  sourceBuildings,
                  destinationBuilding,
                  reservationParty,
                  reservationName));
   }

   @Override
   public HaulDecision takeItemsUntilSatisfied(
         CivilizedVillager villager,
         LoadedBuilding sourceBuilding,
         String reservationParty) {

      List<Container> chests = sourceBuilding.chests();

      List<ItemReservation> itemReservations = sourceBuilding.getReservationsExcluding(reservationParty);

      for (BuildingStockRequirement requirement : requirements) {
         int quota = outstandingAmount(villager, requirement);
         if (quota <= 0)
            continue; // the destination building already has enough, or the villager is carrying the rest of it

         takeForRequirement(villager, requirement, chests, itemReservations, quota);
      }

      if (!allRequirementsSatisfied(villager))
         return HaulDecision.REQUIREMENT_NOT_YET_SATISFIED;

      // whatever was picked up still has to make its way to the destination building
      if (countCarriedItems(villager).hasItems())
         return HaulDecision.REQUIREMENT_SATISFIED_OFFLOAD_ITEMS;

      return HaulDecision.REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO;
   }

   @Override
   protected int outstandingAmount(CivilizedVillager villager, BuildingStockRequirement requirement) {
      if (requirement.insatiable())
         return BuildingStockRequirement.UNLIMITED;

      AggregateItemStack itemsAtDestination =
            requirement.calculateBuildingStock(
                  destinationBuilding.chests(),
                  destinationBuilding.getReservationsExcluding(reservationParty));

      if (requirement.disregardExistingCarriedStock()) {
         return requirement.idealAmount() - itemsAtDestination.getCount();
      }

      AggregateItemStack carried =
            ContainerHelper.countItems(requirement.getHaulInventory(villager), requirement.filter());

      return requirement.idealAmount() - itemsAtDestination.getCount() - carried.getCount();
   }

   @Override
   public String toString() {
      return super.toString() + ": ["
            + sourceBuildings.stream()
                  .map(b -> b.getBuilding().getBuildingType().toString())
                  .collect(Collectors.joining(", "))
            + "] -> " + destinationBuilding.toString();
   }
}
