package com.uncreated.civilized.core.building.requirement;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;

public class SurfaceAreaRequirement implements IBuildingRequirement {

   public static final double VALID_WALL_HEIGHT = 3;
   public static final float PERCENTAGE_VALID_SOLID_WALL = 0.6f;
   private final int requiredArea;

   public SurfaceAreaRequirement(int requiredArea) {
      this.requiredArea = requiredArea;
   }

   @Override
   public RequirementKind kind() {
      return RequirementKind.STRUCTURAL;
   }

   @Override
   public IBuildingRequirementResult evaluate(RequirementContext context) {
      return getResult(context.bounds());
   }

   public Result getResult(BuildingBounds bounds) {

      int x1 = bounds.getUpperCorner().getX();
      int x2 = bounds.getLowerCorner().getX();
      int z1 = bounds.getUpperCorner().getZ();
      int z2 = bounds.getLowerCorner().getZ();

      // add 1 to each rectangle side because bounds include both corners. Measuring sides by subtracting coordinates
      // misses one block that's actually still in the bounds
      int area = (x1 - x2 + 1) * (z1 - z2 + 1);
      return new Result(area, requiredArea);
   }

   public class Result implements IBuildingRequirementResult {
      private final int actualArea;
      private final int allowedArea;

      private Result(int actualArea, int allowedFaults) {
         this.actualArea = actualArea;
         this.allowedArea = allowedFaults;
      }

      @Override
      public boolean isSatisfied() {
         return actualArea >= allowedArea;
      }

      @Override
      public Component getDescription() {
         int numberToDisplay = Math.clamp(actualArea, 0, requiredArea);
         return Component
               .translatable(
                     "menu.building.management.requirements.surface_area.description",
                     numberToDisplay,
                     requiredArea)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable("menu.building.management.requirements.surface_area.tooltip", requiredArea);
      }
   }
}
