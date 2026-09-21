package com.uncreated.civilized.core.building.state;

import net.minecraft.world.item.ItemStack;

public interface IEyeDropperSlotState {

   int getEyeDropperSlotCount();

   ItemStack getEyeDropperSlotItem(int slot);

   boolean isValidEyeDropperSlotItem(int slot, ItemStack stack);

   void setEyeDropperSlotItem(int slot, ItemStack stack);
}
