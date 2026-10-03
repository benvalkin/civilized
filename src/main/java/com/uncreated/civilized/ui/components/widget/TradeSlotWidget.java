package com.uncreated.civilized.ui.components.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.trading.TradeDirection;
import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.core.trading.TradeQuote;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.menu.trading.TradingMenuScreen;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class TradeSlotWidget extends AbstractWidget implements ITradeSlot {

   public static final int SIZE = 16;
   /** The same white overlay vanilla draws over a hovered slot. */
   private static final int SLOT_HIGHLIGHT_COLOR = 0x80FFFFFF;
   /** Darkens the slot's background when there's nothing left to buy. */
   private static final int OUT_OF_STOCK_COLOR = 0x80404040;

   private final TradingMenuScreen parentScreen;
   @Getter
   private final int slot;
   @Getter
   private final TradeItem tradeItem;
   private final Consumer<TradeSlotWidget> onClicked;

   public TradeSlotWidget(
         TradingMenuScreen parentScreen,
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

   /** Selling to a vendor doesn't need any stock, so the slot only looks unavailable while buying. */
   private boolean isOutOfStockToBuy() {
      return parentScreen.getTradeDirection() == TradeDirection.BUY && tradeItem.stock() <= 0;
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      onClicked.accept(this);
   }

   /**
    * The out-of-stock grey overlay needs to be drawn at a custom Z coordinate which is above the item (z ≈ 150) so it
    * greys it out, but below the tooltip (z = 400) so it doesn't cover it.
    */
   private static final int OUT_OF_STOCK_OVERLAY_Z = 180;

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

      SlotFrameRenderer.render(graphics, getX(), getY());

      // out of stock grey overlay - take care to draw under the item so that the item can still be seen
      boolean isOutOfStockToBuy = isOutOfStockToBuy();

      if (isHovered() && !isOutOfStockToBuy)
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

      if (isOutOfStockToBuy)
         graphics.fill(
               RenderType.GUI,
               getX(),
               getY(),
               getX() + SIZE,
               getY() + SIZE,
               OUT_OF_STOCK_OVERLAY_Z,
               OUT_OF_STOCK_COLOR);

      if (isHovered())
         renderTradeTooltip(graphics, mouseX, mouseY);
   }

   // The item's usual tooltip, followed by price/stock etc.

   private void renderTradeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      ItemStack item = tradeItem.item();
      List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), item));
      if (tradeItem.quantityPerTrade() > 1 && !lines.isEmpty()) {
         Component name = lines.getFirst();
         lines.set(
               0,
               Component.translatable("menu.trading.tooltip.quantity_and_name", tradeItem.quantityPerTrade(), name)
                     .withColor(Colors.TEXT_LIGHT_MUTED));
      }
      lines.add(createPriceLine());
      // lines.add(Component.empty());
      lines.add(createStockLine());

      // drawn like vanilla draws an item's tooltip, so things like bundle contents and custom tooltip styles still show
      graphics.renderTooltip(
            parentScreen.getFont(),
            lines,
            item.getTooltipImage(),
            item,
            mouseX,
            mouseY,
            item.get(DataComponents.TOOLTIP_STYLE));
   }

   private Component createPriceLine() {
      ItemStack carried = parentScreen.getMenu().getCarried();
      if (parentScreen.getTradeDirection() == TradeDirection.SELL) {

         if (parentScreen.getMenu().getVendorCurrency().availableCurrency() < tradeItem.sellPrice())
            return Component.translatable("menu.trading.tooltip.vendor_cannot_afford")
                  .withColor(Colors.VALIDATION_ERROR);

         if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, tradeItem.item())) {

            TradeQuote quote =
                  tradeItem.adjustIfOddOrNotAffordable(
                        parentScreen.getMenu().getVendorCurrency().availableCurrency(),
                        parentScreen.getTradeDirection(),
                        carried.getCount());
            Component quantity =
                  Component.literal(String.valueOf(quote.quanity())).withColor(Colors.VALIDATION_SUCCESS);
            if (tradeItem.sellPrice() <= 0)
               return Component.translatable("menu.trading.tooltip.sell_stack_for_nothing", quantity);

            int carriedStackValue = quote.quanity() / tradeItem.quantityPerTrade() * tradeItem.sellPrice();
            return Component
                  .translatable("menu.trading.tooltip.sell_stack_for", quantity, coinsTransaction(carriedStackValue))
                  .withColor(Colors.TEXT_LIGHT_MUTED)
                  .withStyle(ChatFormatting.ITALIC);
         } else
            return coinsTransaction(tradeItem.sellPrice());
      } else
         return coinsTransaction(tradeItem.buyPrice());
   }

   private @NotNull MutableComponent coinsTransaction(int amount) {
      return CurrencyItem.amountTranslation(amount).withColor(Colors.COIN);
   }

   private Component createStockLine() {
      Component stock = Component.literal(String.valueOf(tradeItem.stock())).withColor(Colors.TRADE_STOCK);
      if (tradeItem.stock() > 0)
         return Component.translatable("menu.trading.tooltip.stock", stock).withColor(Colors.TEXT_LIGHT_MUTED);

      return Component.translatable("menu.trading.tooltip.out_of_stock").withColor(Colors.VALIDATION_ERROR);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      defaultButtonNarrationText(narrationElementOutput);
   }
}
