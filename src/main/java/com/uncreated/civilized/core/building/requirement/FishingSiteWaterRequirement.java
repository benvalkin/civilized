package com.uncreated.civilized.core.building.requirement;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

/**
 * Requires a body of water inside the building that is big enough to fish in, i.e. somewhere within the bounds there is
 * a solid block of water of the required width, length and depth.
 */
public class FishingSiteWaterRequirement implements IBuildingRequirement {

   private final int requiredWidth;
   private final int requiredLength;
   private final int requiredDepth;

   public FishingSiteWaterRequirement(int requiredWidth, int requiredLength, int requiredDepth) {
      this.requiredWidth = requiredWidth;
      this.requiredLength = requiredLength;
      this.requiredDepth = requiredDepth;
   }

   public Result getResult(Level level, BuildingBounds bounds) {
      return new Result(containsBodyOfWater(level, bounds));
   }

   private boolean containsBodyOfWater(Level level, BuildingBounds bounds) {

      int sizeX = bounds.getUpperCorner().getX() - bounds.getLowerCorner().getX() + 1;
      int sizeY = bounds.getUpperCorner().getY() - bounds.getLowerCorner().getY() + 1;
      int sizeZ = bounds.getUpperCorner().getZ() - bounds.getLowerCorner().getZ() + 1;

      if (sizeX < requiredWidth || sizeZ < requiredLength || sizeY < requiredDepth)
         return false;

      // the water blocks are read once up front, since each one would otherwise be looked at many times over while
      // checking every position the body of water could start at
      boolean[][][] isWater = new boolean[sizeX][sizeY][sizeZ];
      BlockPos.MutableBlockPos current = bounds.getLowerCorner().mutable();
      for (int x = 0; x < sizeX; x++) {
         for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
               current
                     .set(
                           bounds.getLowerCorner().getX() + x,
                           bounds.getLowerCorner().getY() + y,
                           bounds.getLowerCorner().getZ() + z);
               isWater[x][y][z] = level.getFluidState(current).is(FluidTags.WATER);
            }
         }
      }

      for (int x = 0; x <= sizeX - requiredWidth; x++) {
         for (int y = 0; y <= sizeY - requiredDepth; y++) {
            for (int z = 0; z <= sizeZ - requiredLength; z++) {
               if (isAllWater(isWater, x, y, z))
                  return true;
            }
         }
      }

      return false;
   }

   private boolean isAllWater(boolean[][][] isWater, int startX, int startY, int startZ) {
      for (int x = startX; x < startX + requiredWidth; x++) {
         for (int y = startY; y < startY + requiredDepth; y++) {
            for (int z = startZ; z < startZ + requiredLength; z++) {
               if (!isWater[x][y][z])
                  return false;
            }
         }
      }
      return true;
   }

   public class Result implements IBuildingRequirementResult {
      private final boolean hasBodyOfWater;

      private Result(boolean hasBodyOfWater) {
         this.hasBodyOfWater = hasBodyOfWater;
      }

      @Override
      public boolean isSatisfied() {
         return hasBodyOfWater;
      }

      @Override
      public Component getDescription() {
         return Component.translatable("menu.building.management.requirements.fishing_site_water.description")
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component
               .translatable(
                     "menu.building.management.requirements.fishing_site_water.tooltip",
                     requiredWidth,
                     requiredLength,
                     requiredDepth);
      }
   }
}
