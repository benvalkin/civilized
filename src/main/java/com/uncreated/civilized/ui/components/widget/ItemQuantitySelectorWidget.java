package com.uncreated.civilized.ui.components.widget;

import java.util.function.Consumer;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class ItemQuantitySelectorWidget extends ItemDisplayWidget {

   private final Consumer<Integer> onAmountChanged;

   public ItemQuantitySelectorWidget(
         int x,
         int y,
         ItemStack initialItem,
         int initialAmount,
         Consumer<Integer> onAmountChanged) {
      super(x, y, initialItem);
      this.onAmountChanged = onAmountChanged;
      this.slot.getItem().setCount(initialAmount); // need to explicitly set the count otherwise stack may be capped at
                                                   // item's max stack size
   }

   public void setItemAndQuantity(ItemStack itemStack, int quantity) {
      slot.set(itemStack.copy());
      this.slot.getItem().setCount(quantity);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {

      boolean isShiftKeyPressed = Screen.hasShiftDown();
      boolean isCtrlPressed = Screen.hasControlDown();

      int change = 1;
      if (isShiftKeyPressed)
         change = 10;
      if (isCtrlPressed)
         change = 100;

      int direction = button == 0 ? 1 : -1;

      int changeSigned = change * direction;
      int newQuantity = slot.getItem().getCount() + changeSigned;

      newQuantity = Math.clamp(newQuantity, 1, 999);
      slot.getItem().setCount(newQuantity);

      onAmountChanged.accept(slot.getItem().getCount());

      return true;
   }
}
