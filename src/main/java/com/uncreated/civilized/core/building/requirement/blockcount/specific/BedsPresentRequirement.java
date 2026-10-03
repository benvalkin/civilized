package com.uncreated.civilized.core.building.requirement.blockcount.specific;

import java.util.Collections;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.validators.BlockTagValidator;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;

public class BedsPresentRequirement extends BlockCountRequirement {
   public BedsPresentRequirement(int minBlocks, boolean hideIfSatisfied) {
      this(minBlocks, null, hideIfSatisfied);
   }

   public BedsPresentRequirement(int minBlocks, @Nullable Integer maxBlocks, boolean hideIfSatisfied) {
      super(
            new BlockTagValidator(Collections.singletonList(BlockTags.BEDS)),
            minBlocks,
            maxBlocks,
            Component.translatable("menu.building.management.requirements.count.description.beds"),
            hideIfSatisfied);
   }
}
