package com.uncreated.civilized.core.building.requirement;

import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.chat.Component;

public class TownHallLevelRequirement implements IBuildingRequirement {

   private final int requiredLevel;

   public TownHallLevelRequirement(int requiredLevel) {
      this.requiredLevel = requiredLevel;
   }

   @Override
   public RequirementKind kind() {
      return RequirementKind.PREREQUISITE;
   }

   @Override
   public IBuildingRequirementResult evaluate(RequirementContext context) {
      return getResult(context.townHallLevel());
   }

   public Result getResult(int actualLevel) {

      return new Result(actualLevel, requiredLevel);
   }

   public class Result implements IBuildingRequirementResult {

      private final int actualLevel;
      @Getter
      private final int requiredLevel;

      private Result(int actualLevel, int requiredLevel) {
         this.actualLevel = actualLevel;
         this.requiredLevel = requiredLevel;
      }

      @Override
      public boolean isSatisfied() {
         return actualLevel >= requiredLevel;
      }

      @Override
      public Component getDescription() {
         return Component
               .translatable(
                     "menu.building.management.requirements.town_hall_level.description",
                       requiredLevel)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable("menu.building.management.requirements.town_hall_level.tooltip", requiredLevel);
      }
   }
}
