package com.uncreated.civilized.core.building.requirement.registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.requirement.IBuildingRequirement;
import com.uncreated.civilized.core.building.BuildingType;

import lombok.Builder;
import lombok.Getter;

@Builder(builderMethodName = "forBuilding", buildMethodName = "create")
public class BuildingRequirementList implements Iterable<IBuildingRequirement> {
   private final List<IBuildingRequirement> requirements;
   @Getter
   private final BuildingType buildingType;
   @Getter
   private final int upgradeLevel;

   public static BuildingRequirementListBuilder forBuilding(BuildingType buildingType, int upgradeLevel) {
      return new BuildingRequirementListBuilder().buildingType(buildingType).upgradeLevel(upgradeLevel);
   }

   public static class BuildingRequirementListBuilder {
      private List<IBuildingRequirement> requirements = new ArrayList<>();

      public BuildingRequirementListBuilder add(IBuildingRequirement requirement) {
         requirements.add(requirement);
         return this;
      }
   }

   @Override
   public @NotNull Iterator<IBuildingRequirement> iterator() {
      return requirements.iterator();
   }

   @Override
   public void forEach(Consumer<? super IBuildingRequirement> action) {
      requirements.forEach(action);
   }
}
