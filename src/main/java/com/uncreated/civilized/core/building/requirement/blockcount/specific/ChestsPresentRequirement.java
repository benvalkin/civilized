package com.uncreated.civilized.core.building.requirement.blockcount.specific;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.validators.BlockClassValidator;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractChestBlock;

public class ChestsPresentRequirement extends BlockCountRequirement {
   public ChestsPresentRequirement(int minBlocks, boolean hideIfSatisfied) {
      this(minBlocks, null, hideIfSatisfied);
   }

   public ChestsPresentRequirement(int minBlocks, @Nullable Integer maxBlocks, boolean hideIfSatisfied) {
      super(
            new BlockClassValidator(AbstractChestBlock.class),
            minBlocks,
            maxBlocks,
            Component.translatable("menu.building.management.requirements.count.description.chests"),
            hideIfSatisfied);
   }
}
