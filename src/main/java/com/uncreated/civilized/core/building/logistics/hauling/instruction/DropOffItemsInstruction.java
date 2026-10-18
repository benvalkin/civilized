package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;
import java.util.function.Function;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.experimental.Accessors;
import net.minecraft.world.Container;

@Accessors(fluent = true)
public record DropOffItemsInstruction(LoadedBuilding destinationBuilding,
      List<Function<CivilizedVillager, Container>> inventoriesToOffload) implements IHaulingInstruction {

   @Override
   public String toString() {
      return "[" + inventoriesToOffload.size() + " inventories] -> " + destinationBuilding;
   }
}
