package com.uncreated.civilized.core.building.logistics.hauling.instruction;

import java.util.List;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;

import com.uncreated.civilized.entity.CivilizedVillager;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public abstract class ConditionalHaulingInstruction<T extends ItemStockRequirement> implements IHaulingInstruction {
   protected final T requirement;
   protected final List<LoadedBuilding> sourceBuildings;
   protected int currentBuildingIndex;

   protected ConditionalHaulingInstruction(T requirement, List<LoadedBuilding> sourceBuildings) {
      this.requirement = requirement;
      this.sourceBuildings = sourceBuildings;
      this.currentBuildingIndex = 0;

      if (sourceBuildings.isEmpty()) {
         throw new IllegalArgumentException("sourceBuildings cannot be empty");
      }
   }

   public abstract HaulDecision takeItemsUntilSatisfied(
           CivilizedVillager villager,
           LoadedBuilding sourceBuilding);

   @Override
   public String toString() {
      return requirement.toString();
   }

   public enum HaulDecision {
      REQUIREMENT_NOT_YET_SATISFIED, REQUIREMENT_SATISFIED_OFFLOAD_ITEMS, REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO
   }
}
