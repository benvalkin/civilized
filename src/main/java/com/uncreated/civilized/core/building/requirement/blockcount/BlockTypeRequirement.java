package com.uncreated.civilized.core.building.requirement.blockcount;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.validators.BlockTypeValidator;

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
   public boolean haltChecksIfBlockCanSeeSky() {
      return true;
   }
}
