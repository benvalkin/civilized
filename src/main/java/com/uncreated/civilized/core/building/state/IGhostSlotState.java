package com.uncreated.civilized.core.building.state;

import net.minecraft.world.item.ItemStack;

public interface IGhostSlotState {

   int getGhostSlotCount();

   ItemStack getGhostSlotItem(int slot);

   boolean isValidGhostSlotItem(int slot, ItemStack stack);

   void setGhostSlotItem(int slot, ItemStack stack);
}
