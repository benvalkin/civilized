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
}
