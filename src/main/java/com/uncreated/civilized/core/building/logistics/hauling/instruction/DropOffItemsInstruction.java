package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;
import java.util.stream.Collectors;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;

import lombok.experimental.Accessors;

@Accessors(fluent = true)
public record DropOffItemsInstruction(LoadedBuilding destinationBuilding,
      List<VillagerInventoryType> inventoriesToOffload) implements IHaulingInstruction {

   @Override
   public String toString() {
      return "[" + inventoriesToOffload.stream().map(Enum::toString).collect(Collectors.joining(", ")) + "] -> "
            + destinationBuilding;
   }
}
