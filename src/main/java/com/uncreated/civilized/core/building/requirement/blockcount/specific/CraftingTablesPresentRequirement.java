package com.uncreated.civilized.core.building.requirement.blockcount.specific;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.blockcount.BlockCountRequirement;
import com.uncreated.civilized.core.building.requirement.blockcount.validators.BlockClassValidator;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.CraftingTableBlock;

public class CraftingTablesPresentRequirement extends BlockCountRequirement {
   public CraftingTablesPresentRequirement(int minBlocks, boolean hideIfSatisfied) {
      this(minBlocks, null, hideIfSatisfied);
   }

   public CraftingTablesPresentRequirement(int minBlocks, @Nullable Integer maxBlocks, boolean hideIfSatisfied) {
      super(
            new BlockClassValidator(CraftingTableBlock.class),
            minBlocks,
            maxBlocks,
            Component.translatable("menu.building.management.requirements.count.description.crafting_tables"),
            hideIfSatisfied);
   }
}
