package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;

@Accessors(fluent = true)
@Getter
public class TakeToInventoryInstruction extends ConditionalHaulingInstruction<InventoryStockRequirement> {
   protected TakeToInventoryInstruction(InventoryStockRequirement requirement, List<LoadedBuilding> sourceBuildings) {
      super(requirement, sourceBuildings);
   }

   public static Optional<TakeToInventoryInstruction> tryCreate(
         InventoryStockRequirement requirement,
         List<LoadedBuilding> candidateBuildings) {

      ItemStockRequirement.StockResult stockResult = requirement.evaluate(candidateBuildings);
      if (stockResult.satisfied()) {
         TakeToInventoryInstruction instruction = new TakeToInventoryInstruction(requirement, candidateBuildings);
         return Optional.of(instruction);
      }

      return Optional.empty();
   }

   public HaulDecision takeItemsUntilSatisfied(CivilizedVillager villager, LoadedBuilding building) {

      Container inventory = requirement.getHaulInventory(villager);

      AggregateItemStack itemsAlreadyInInventory = ContainerHelper.countItems(inventory, requirement.filter());

      if (itemsAlreadyInInventory.getCount() >= requirement().idealAmount())
         return HaulDecision.REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO; // villager already has enough items

      int quota = requirement().idealAmount() - itemsAlreadyInInventory.getCount();
      int taken = 0;

      List<Container> chests = building.findChests();
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
   }

   @Override
   public String toString() {
      return requirement.toString() + ": ["
            + sourceBuildings.stream()
                  .map(b -> b.getBuilding().getBuildingType().toString())
                  .collect(Collectors.joining(", "))
            + "] -> " + requirement.inventoryType();
   }
}
