package com.uncreated.civilized.ui.components.widget;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A slot in the trading screen's grid with no trade in it. It's only drawn, and can't be clicked. */
public class EmptyTradeSlotWidget extends AbstractWidget implements ITradeSlot {

   @Getter
   private final int slot;

   public EmptyTradeSlotWidget(int x, int y, int slot) {
      super(x, y, TradeSlotWidget.SIZE, TradeSlotWidget.SIZE, Component.empty());
      this.slot = slot;
      // inactive widgets ignore clicks, and don't make a sound when clicked
      this.active = false;
   }

   @Override
   public @Nullable TradeItem getTradeItem() {
      return null;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      SlotFrameRenderer.render(graphics, getX(), getY());
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }
}
