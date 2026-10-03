package com.uncreated.civilized.core.building.requirement.blockcount;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirement;
import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class BlockCountRequirement implements IBuildingRequirement {

   protected final IBlockValidator validator;
   protected final int minBlocks;
   /** The most blocks allowed, or null if there's no limit. */
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
      return new BlockCountResult(actualBlocks, minBlocks);
   }

   public class BlockCountResult implements IBuildingRequirementResult {
      protected final int actualBlocks;
      protected final int requiredBlocks;

      public BlockCountResult(int actualBlocks, int requiredBlocks) {
         this.actualBlocks = actualBlocks;
         this.requiredBlocks = requiredBlocks;
      }

      @Override
      public boolean isSatisfied() {
         return actualBlocks >= requiredBlocks;
      }

      @Override
      public Component getDescription() {
         int numberToDisplay = Math.clamp(actualBlocks, 0, requiredBlocks);
         return Component
               .translatable(
                     "menu.building.management.requirements.count.description",
                     blockDescription,
                     numberToDisplay,
                     requiredBlocks)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component
               .translatable("menu.building.management.requirements.count.tooltip", requiredBlocks, blockDescription);
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
