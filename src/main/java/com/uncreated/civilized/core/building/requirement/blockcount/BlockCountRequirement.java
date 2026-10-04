package com.uncreated.civilized.core.building.requirement.blockcount;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirement;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.core.building.requirement.RequirementContext;
import com.uncreated.civilized.core.building.requirement.RequirementKind;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class BlockCountRequirement implements IBuildingRequirement {

   protected final IBlockValidator validator;
   protected final int minBlocks;
   protected final @Nullable Integer maxBlocks;
   protected final Component blockDescription;
   private final boolean hidIfSatisfied;

   public BlockCountRequirement(
         IBlockValidator validator,
         int minBlocks,
         Component blockDescription,
         boolean hideIfSatisfied) {
      this(validator, minBlocks, null, blockDescription, hideIfSatisfied);
   }

   public BlockCountRequirement(
         IBlockValidator validator,
         int minBlocks,
         @Nullable Integer maxBlocks,
         Component blockDescription,
         boolean hideIfSatisfied) {
      this.validator = validator;
      this.minBlocks = minBlocks;
      this.maxBlocks = maxBlocks;
      this.blockDescription = blockDescription;
      this.hidIfSatisfied = hideIfSatisfied;
   }

   @Override
   public RequirementKind kind() {
      return RequirementKind.STRUCTURAL;
   }

   @Override
   public IBuildingRequirementResult evaluate(RequirementContext context) {
      return getResult(context.level(), context.bounds());
   }

   public BlockCountResult getResult(Level level, BuildingBounds bounds) {
      int validBlocksFound = 0;
      BlockPos.MutableBlockPos current = bounds.getLowerCorner().mutable();
      for (int x = bounds.getLowerCorner().getX(); x <= bounds.getUpperCorner().getX(); x++) {
         for (int z = bounds.getLowerCorner().getZ(); z <= bounds.getUpperCorner().getZ(); z++) {
            for (int y = bounds.getLowerCorner().getY(); y <= bounds.getUpperCorner().getY(); y++) {
               current.set(x, y, z);

               if (validator.isBlockValid(current, level))
                  validBlocksFound++;

               if (haltChecksIfBlockCanSeeSky() && level.canSeeSky(current))
                  break;
            }
         }
      }
      return createResult(validBlocksFound);
   }

   public boolean haltChecksIfBlockCanSeeSky() {
      return false;
   }

   public BlockCountResult createResult(int actualBlocks) {
      return new BlockCountResult(actualBlocks, minBlocks, maxBlocks);
   }

   public class BlockCountResult implements IBuildingRequirementResult {
      protected final int actualBlocks;
      protected final int minBlocks;
      @Nullable
      private final Integer maxBlocks;

      public BlockCountResult(int actualBlocks, int minBlocks, @Nullable Integer maxBlocks) {
         this.actualBlocks = actualBlocks;
         this.minBlocks = minBlocks;
         this.maxBlocks = maxBlocks;
      }

      @Override
      public boolean isSatisfied() {
         if (maxBlocks == null)
            return actualBlocks >= minBlocks;

         return actualBlocks >= minBlocks && actualBlocks <= maxBlocks;
      }

      @Override
      public Component getDescription() {

         if (maxBlocks == null || minBlocks == maxBlocks) {
            int numberToDisplay = Math.clamp(actualBlocks, 0, minBlocks);
            return Component
                  .translatable(
                        "menu.building.management.requirements.count.description",
                        blockDescription,
                        numberToDisplay,
                        minBlocks)
                  .withColor(Colors.MENU_TEXT_DARK);
         }

         return Component
               .translatable(
                     "menu.building.management.requirements.count.range.description",
                     blockDescription,
                     actualBlocks,
                     minBlocks,
                     maxBlocks)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         if (maxBlocks == null)
            return Component
                  .translatable("menu.building.management.requirements.count.min.tooltip", minBlocks, blockDescription);

         if (minBlocks == maxBlocks)
            return Component.translatable(
                  "menu.building.management.requirements.count.exact.tooltip",
                  minBlocks,
                  blockDescription);

         return Component.translatable(
               "menu.building.management.requirements.count.range.tooltip",
               minBlocks,
               maxBlocks,
               blockDescription);
      }

      @Override
      public boolean hideIfSatisfied() {
         return hidIfSatisfied;
      }
   }

   public interface IBlockValidator {
      boolean isBlockValid(BlockPos pos, Level level);
   }
}
