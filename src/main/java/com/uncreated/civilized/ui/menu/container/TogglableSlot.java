package com.uncreated.civilized.ui.menu.container;

import java.util.function.BooleanSupplier;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * A container slot that can be hidden. Intended to be used for menus that only need to display player inventory slots
 * under certain conditions (e.g. if a certain tab is showing). A hidden slot is physically present, but is neither
 * drawn nor able to be clicked.
 */
public class TogglableSlot extends Slot {

   private final BooleanSupplier isVisible;

   public TogglableSlot(Container container, int slotIndex, int x, int y, BooleanSupplier isVisible) {
      super(container, slotIndex, x, y);
      this.isVisible = isVisible;
   }

   @Override
   public boolean isActive() {
      return isVisible.getAsBoolean();
   }
}
