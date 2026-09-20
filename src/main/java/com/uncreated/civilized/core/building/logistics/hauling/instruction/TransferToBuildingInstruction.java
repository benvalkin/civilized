package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
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
         BuildingStockRequirement requirement,
         List<LoadedBuilding> sourceBuildings,
         LoadedBuilding destinationBuilding) {
      super(requirement, sourceBuildings);
      this.destinationBuilding = destinationBuilding;
   }

   public static Optional<TransferToBuildingInstruction> tryCreate(
         BuildingStockRequirement requirement,
         LoadedBuilding destinationBuilding,
         List<LoadedBuilding> sourceBuildings) {

      BuildingStockRequirement.StockResult stockResult = requirement.evaluate(sourceBuildings);
      if (stockResult.satisfied()) {
         TransferToBuildingInstruction instruction =
               new TransferToBuildingInstruction(requirement, sourceBuildings, destinationBuilding);
         return Optional.of(instruction);
      }

      return Optional.empty();
   }

   public HaulDecision takeItemsUntilSatisfied(CivilizedVillager villager, LoadedBuilding sourceBuilding) {

      Container inventory = requirement.getHaulInventory(villager);
      List<Container> destinationBuildingChests = destinationBuilding.findChests();

      if (!requirement.insatiable()) {

         AggregateItemStack itemsAlreadyAtDestinationBuilding =
               requirement.calculateBuildingStock(
                     destinationBuildingChests,
                     destinationBuilding.getItemReservations().values());

         if (itemsAlreadyAtDestinationBuilding.getCount() >= requirement.idealAmount())
            return HaulDecision.REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO;

         AggregateItemStack itemsAlreadyInInventory = ContainerHelper.countItems(inventory, requirement.filter());
         AggregateItemStack grandExistingTotal =
               new AggregateItemStack(itemsAlreadyInInventory, itemsAlreadyAtDestinationBuilding);

         if (grandExistingTotal.getCount() >= requirement.idealAmount())
            // we can fulfill the requirement if we offload our inventory
            return HaulDecision.REQUIREMENT_SATISFIED_OFFLOAD_ITEMS;

         int quota = requirement().idealAmount() - itemsAlreadyInInventory.getCount();
         int taken = 0;

         List<Container> chests = sourceBuilding.findChests();
         for (Container source : chests) {
            int transferred = ContainerHelper.transferNicely(source, inventory, requirement().filter(), quota);

            quota -= transferred;
            taken += transferred;

            if (quota <= 0)
               break;
         }

         int updatedItemsInInventory = itemsAlreadyInInventory.getCount() + taken;
         if (updatedItemsInInventory >= requirement.idealAmount())
            return HaulDecision.REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO;

         return HaulDecision.REQUIREMENT_NOT_YET_SATISFIED;
      } else {
         List<Container> chests = sourceBuilding.findChests();
         for (Container source : chests) {
            ContainerHelper.transferNicely(source, inventory, requirement().filter(), Integer.MAX_VALUE);
         }

         return HaulDecision.REQUIREMENT_NOT_YET_SATISFIED;
      }
   }

   @Override
   public String toString() {
      return requirement.toString() + ": ["
            + sourceBuildings.stream()
                  .map(b -> b.getBuilding().getBuildingType().toString())
                  .collect(Collectors.joining(", "))
            + "] -> " + destinationBuilding.toString();
   }
}
