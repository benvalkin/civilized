package com.uncreated.civilized.core.building.requirement.blockcount.specific;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;

public class BeehivesPresentRequirement extends BlockCountRequirement {

   public BeehivesPresentRequirement(int minBlocks, boolean hideIfSatisfied) {
      this(minBlocks, null, hideIfSatisfied);
   }

   public BeehivesPresentRequirement(int minBlocks, @Nullable Integer maxBlocks, boolean hideIfSatisfied) {
      super(
            (pos, level) -> level.getBlockState(pos).getBlock() instanceof BeehiveBlock
                  && CampfireBlock.isSmokeyPos(level, pos),
            minBlocks,
            maxBlocks,
            Component.translatable("menu.building.management.requirements.beehives_with_campfire.description"),
            hideIfSatisfied);
   }

   @Override
   public BlockCountResult createResult(int actualBlocks) {
      return new Result(actualBlocks, minBlocks);
   }

   public class Result extends BlockCountResult {

      public Result(int actualBlocks, int requiredBlocks) {
         super(actualBlocks, requiredBlocks);
      }

      @Override
      public boolean isSatisfied() {
         return super.isSatisfied() && (maxBlocks == null || actualBlocks <= maxBlocks);
      }

      @Override
      public Component getDescription() {
         if (maxBlocks == null)
            return super.getDescription();

         return Component
               .translatable(
                     "menu.building.management.requirements.count.range.description",
                     blockDescription,
                     actualBlocks,
                     requiredBlocks,
                     maxBlocks)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         if (maxBlocks == null)
            return super.getTooltipDescription();

         return Component.translatable(
               "menu.building.management.requirements.beehives_with_campfire.tooltip",
               requiredBlocks,
               maxBlocks,
               blockDescription);
      }
   }
}
