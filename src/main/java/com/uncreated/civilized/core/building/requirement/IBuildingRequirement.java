package com.uncreated.civilized.core.building.requirement;

public interface IBuildingRequirement {

   RequirementKind kind();

   IBuildingRequirementResult evaluate(RequirementContext context);

   default boolean hideIfSatisfied() {
      return false;
   }
}
