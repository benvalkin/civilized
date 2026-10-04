package com.uncreated.civilized.core.building.requirement;

import java.util.Set;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class SpaceRequirement implements IBuildingRequirement {

   private final int requiredSpace;

   public SpaceRequirement(int requiredSpace) {
      this.requiredSpace = requiredSpace;
   }

   @Override
   public RequirementKind kind() {
      return RequirementKind.STRUCTURAL;
   }

   @Override
   public IBuildingRequirementResult evaluate(RequirementContext context) {
      return getResult(context.level(), context.bounds());
   }

   public Result getResult(Level level, BuildingBounds bounds) {

      return new Result(bounds.findValidInsideFloorBlocks(level), requiredSpace);
   }

   public class Result implements IBuildingRequirementResult {
      @Getter
      private final Set<BlockPos> validFloorBlocks;
      private final int requiredSpace;

      private Result(Set<BlockPos> validFloorBlocks, int requiredSpace) {
         this.validFloorBlocks = validFloorBlocks;
         this.requiredSpace = requiredSpace;
      }

      @Override
      public boolean isSatisfied() {
         return validFloorBlocks.size() >= requiredSpace;
      }

      @Override
      public Component getDescription() {
         int numberToDisplay = Math.clamp(validFloorBlocks.size(), 0, requiredSpace);
         return Component
               .translatable("menu.building.management.requirements.space.description", numberToDisplay, requiredSpace)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable("menu.building.management.requirements.space.tooltip", requiredSpace);
      }
   }
}
