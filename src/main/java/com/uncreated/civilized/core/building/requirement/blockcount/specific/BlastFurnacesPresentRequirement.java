package com.uncreated.civilized.core.building.requirement.blockcount.specific;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.validators.BlockClassValidator;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BlastFurnaceBlock;

public class BlastFurnacesPresentRequirement extends BlockCountRequirement {
   public BlastFurnacesPresentRequirement(int minBlocks, boolean hideIfSatisfied) {
      this(minBlocks, null, hideIfSatisfied);
   }

   public BlastFurnacesPresentRequirement(int minBlocks, @Nullable Integer maxBlocks, boolean hideIfSatisfied) {
      super(
            new BlockClassValidator(BlastFurnaceBlock.class),
            minBlocks,
            maxBlocks,
            Component.translatable("menu.building.management.requirements.count.description.blast_furnaces"),
            hideIfSatisfied);
   }
}
