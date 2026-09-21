package com.uncreated.civilized.ui.menu.building.worksite.grove.items;

import com.uncreated.civilized.ui.menu.item.management.LegacyEyedropperSlot;

import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SaplingBlock;

public class SaplingLegacyEyedropperSlot extends LegacyEyedropperSlot {
   public SaplingLegacyEyedropperSlot(Container container, int slot, int x, int y) {
      super(container, slot, x, y);
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      if (!(stack.getItem() instanceof BlockItem blockItem))
         return false;

      if (!(blockItem.getBlock() instanceof SaplingBlock))
         return false;

      return true;
   }
}
