package com.uncreated.civilized.core.building.requirement;

import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.chat.Component;

public class PopulationRequirement implements IBuildingRequirement {

   private final int requiredPopulation;

   public PopulationRequirement(int requiredPopulation) {
      this.requiredPopulation = requiredPopulation;
   }

   @Override
   public RequirementKind kind() {
      return RequirementKind.PREREQUISITE;
   }

   @Override
   public IBuildingRequirementResult evaluate(RequirementContext context) {
      return getResult(context.population());
   }

   public Result getResult(int actualPopulation) {

      return new Result(actualPopulation, requiredPopulation);
   }

   public class Result implements IBuildingRequirementResult {

      private final int actualPopulation;
      @Getter
      private final int requiredPopulation;

      private Result(int actualPopulation, int requiredPopulation) {
         this.actualPopulation = actualPopulation;
         this.requiredPopulation = requiredPopulation;
      }

      @Override
      public boolean isSatisfied() {
         return actualPopulation >= requiredPopulation;
      }

      @Override
      public Component getDescription() {
         int numberToDisplay = Math.clamp(actualPopulation, 0, requiredPopulation);
         return Component
               .translatable(
                     "menu.building.management.requirements.population.description",
                     numberToDisplay,
                     requiredPopulation)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable("menu.building.management.requirements.population.tooltip", requiredPopulation);
      }
   }
}
