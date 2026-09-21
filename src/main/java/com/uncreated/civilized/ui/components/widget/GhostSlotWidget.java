package com.uncreated.civilized.ui.components.widget;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class GhostSlotWidget extends AbstractWidget {

   public static final int SIZE = 16;
   /** The same white overlay vanilla draws over a hovered slot. */
   private static final int SLOT_HIGHLIGHT_COLOR = 0x80FFFFFF;
   private static final int SLOT_BACKGROUND_COLOR = 0xFF8B8B8B;
   private static final int SLOT_SHADOW_COLOR = 0xFF373737;
   private static final int SLOT_LIGHT_COLOR = 0xFFFFFFFF;

   private final Supplier<ItemStack> currentItem;
   private final Consumer<ItemStack> onItemChosen;
   private final Predicate<ItemStack> isItemAllowed;

   @Getter
   private final AbstractContainerScreen<?> parentScreen;

   public GhostSlotWidget(
         AbstractContainerScreen<?> parentScreen,
         int x,
         int y,
         Supplier<ItemStack> currentItem,
         Predicate<ItemStack> isItemAllowed,
         Consumer<ItemStack> onItemChosen) {
      super(x, y, SIZE, SIZE, Component.empty());
      this.parentScreen = parentScreen;
      this.currentItem = currentItem;
      this.isItemAllowed = isItemAllowed;
      this.onItemChosen = onItemChosen;
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      // the stack the player is dragging around belongs to the menu, and is only read here, never changed
      ItemStack carried = parentScreen.getMenu().getCarried();

      if (carried.isEmpty()) {
         onItemChosen.accept(ItemStack.EMPTY);
         return;
      }

      if (!isItemAllowed.test(carried))
         return;

      onItemChosen.accept(carried.copyWithCount(1));
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      ItemStack item = currentItem.get();

      renderSlotFrame(graphics);

      if (isHovered())
         graphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, SLOT_HIGHLIGHT_COLOR);

      if (item.isEmpty())
         return;

      // drawn without a count
      graphics.renderFakeItem(item, getX(), getY());

      if (isHovered())
         graphics.renderTooltip(Minecraft.getInstance().font, item, mouseX, mouseY);
   }

   /**
    * Draws the same sunken square vanilla uses for inventory slots, so the slot can be seen while it is empty.
    */
   private void renderSlotFrame(GuiGraphics graphics) {
      int left = getX() - 1;
      int top = getY() - 1;
      int right = getX() + SIZE + 1;
      int bottom = getY() + SIZE + 1;

      graphics.fill(left, top, right, bottom, SLOT_BACKGROUND_COLOR);
      graphics.fill(left, top, right - 1, top + 1, SLOT_SHADOW_COLOR);
      graphics.fill(left, top, left + 1, bottom - 1, SLOT_SHADOW_COLOR);
      graphics.fill(left + 1, bottom - 1, right, bottom, SLOT_LIGHT_COLOR);
      graphics.fill(right - 1, top + 1, right, bottom, SLOT_LIGHT_COLOR);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
