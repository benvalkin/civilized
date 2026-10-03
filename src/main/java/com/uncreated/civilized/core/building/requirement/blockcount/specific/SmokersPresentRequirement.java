package com.uncreated.civilized.core.building.requirement.blockcount.specific;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.validators.BlockClassValidator;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.SmokerBlock;

public class SmokersPresentRequirement extends BlockCountRequirement {
   public SmokersPresentRequirement(int minBlocks, boolean hideIfSatisfied) {
      this(minBlocks, null, hideIfSatisfied);
   }

   public SmokersPresentRequirement(int minBlocks, @Nullable Integer maxBlocks, boolean hideIfSatisfied) {
      super(
            new BlockClassValidator(SmokerBlock.class),
            minBlocks,
            maxBlocks,
            Component.translatable("menu.building.management.requirements.count.description.smokers"),
            hideIfSatisfied);
   }
}
