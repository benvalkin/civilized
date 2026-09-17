package com.uncreated.civilized.core.building.requirement.blockcount.specific;

import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;

public class BeehivesPresentRequirement extends BlockCountRequirement {

   private final int maximumBlocks;

   public BeehivesPresentRequirement(int requiredBlocks, int maximumBlocks, boolean hideIfSatisfied) {
      super(
            (pos, level) -> level.getBlockState(pos).getBlock() instanceof BeehiveBlock
                  && CampfireBlock.isSmokeyPos(level, pos),
            requiredBlocks,
            Component.translatable("menu.building.management.requirements.beehives_with_campfire.description"),
            hideIfSatisfied);
      this.maximumBlocks = maximumBlocks;
   }

   @Override
   public BlockCountResult createResult(int actualBlocks) {
      return new Result(actualBlocks, requiredBlocks);
   }

   public class Result extends BlockCountResult {

      public Result(int actualBlocks, int requiredBlocks) {
         super(actualBlocks, requiredBlocks);
      }

      @Override
      public boolean isSatisfied() {
         return actualBlocks >= requiredBlocks && actualBlocks <= maximumBlocks;
      }

      @Override
      public Component getDescription() {
         return Component
               .translatable(
                     "menu.building.management.requirements.count.range.description",
                     blockDescription,
                     actualBlocks,
                     requiredBlocks,
                     maximumBlocks)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable(
               "menu.building.management.requirements.beehives_with_campfire.tooltip",
               requiredBlocks,
               maximumBlocks,
               blockDescription);
      }
   }
}
