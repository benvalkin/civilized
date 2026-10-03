package com.uncreated.civilized.core.building.requirement.blockcount;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.validators.BlockTypeValidator;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;

public class BlockTypeRequirement extends BlockCountRequirement {
   private final BuildingBlockTypes.BuildingBlockType type;

   public BlockTypeRequirement(BuildingBlockTypes.BuildingBlockType type, int minBlocks) {
      this(type, minBlocks, null);
   }

   public BlockTypeRequirement(BuildingBlockTypes.BuildingBlockType type, int minBlocks, @Nullable Integer maxBlocks) {
      super(new BlockTypeValidator(type), minBlocks, maxBlocks, type.getDescription(), false);
      this.type = type;
   }

   @Override
   public BlockCountResult createResult(int actualBlocks) {
      return new Result(actualBlocks, minBlocks);
   }

   @Override
   public boolean haltChecksIfBlockCanSeeSky() {
      return true;
   }

   public class Result extends BlockCountResult {

      public Result(int actualBlocks, int requiredBlocks) {
         super(actualBlocks, requiredBlocks);
      }

      @Override
      public Component getDescription() {
         int numberToDisplay = Math.clamp(actualBlocks, 0, requiredBlocks);
         return Component
               .translatable(
                     "menu.building.management.requirements.count.description",
                     type.getDescription(),
                     numberToDisplay,
                     requiredBlocks)
               .withColor(Colors.MENU_TEXT_DARK);
      }

      @Override
      public Component getTooltipDescription() {
         return Component.translatable(
               "menu.building.management.requirements.count.tooltip",
               requiredBlocks,
               type.getDescription());
      }
   }
}
