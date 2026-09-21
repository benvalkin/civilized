package com.uncreated.civilized.ui.menu.building.worksite.cropfarm.items;

import com.uncreated.civilized.ui.menu.item.management.LegacyEyedropperSlot;

import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;

public class CropLegacyEyedropperSlot extends LegacyEyedropperSlot {
   public CropLegacyEyedropperSlot(Container container, int slot, int x, int y) {
      super(container, slot, x, y);
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      if (!(stack.getItem() instanceof BlockItem blockItem))
         return false;

      if (!(blockItem.getBlock() instanceof CropBlock))
         return false;

      return true;
   }
}
