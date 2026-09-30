package com.uncreated.civilized.ui.components.widget;

import java.util.function.Consumer;

import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public class TradeSlotWidget extends AbstractWidget {

   public static final int SIZE = 16;
   /** The same white overlay vanilla draws over a hovered slot. */
   private static final int SLOT_HIGHLIGHT_COLOR = 0x80FFFFFF;

   private final AbstractContainerScreen<?> parentScreen;
   @Getter
   private final int slot;
   @Getter
   private final TradeItem tradeItem;
   private final Consumer<TradeSlotWidget> onClicked;

   public TradeSlotWidget(
         AbstractContainerScreen<?> parentScreen,
         int x,
         int y,
         int slot,
         TradeItem tradeItem,
         Consumer<TradeSlotWidget> onClicked) {
      super(x, y, SIZE, SIZE, Component.empty());
      this.slot = slot;
      this.tradeItem = tradeItem;
      this.parentScreen = parentScreen;
      this.onClicked = onClicked;
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      onClicked.accept(this);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

      SlotFrameRenderer.render(graphics, getX(), getY());

      if (isHovered())
         graphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, SLOT_HIGHLIGHT_COLOR);

      if (tradeItem.item().isEmpty())
         return;

      graphics.renderItem(tradeItem.item(), getX(), getY());
      if (tradeItem.quantityPerTrade() > 1) {
         graphics.renderItemDecorations(
               parentScreen.getFont(),
               tradeItem.item(),
               getX(),
               getY(),
               String.valueOf(tradeItem.quantityPerTrade()));
      }

      if (isHovered())
         graphics.renderTooltip(Minecraft.getInstance().font, tradeItem.item(), mouseX, mouseY);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
