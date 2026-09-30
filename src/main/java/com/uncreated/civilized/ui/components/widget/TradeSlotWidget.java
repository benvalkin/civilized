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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class TradeSlotWidget extends AbstractWidget {

   public static final int SIZE = 16;
   /** The same white overlay vanilla draws over a hovered slot. */
   private static final int SLOT_HIGHLIGHT_COLOR = 0x80FFFFFF;

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
      if (parentScreen.getTradeDirection() == TradeDirection.SELL && !carried.isEmpty()
            && ItemStack.isSameItemSameComponents(carried, tradeItem.item())) {
         TradeQuote quote =
               tradeItem.adjustIfOddOrNotAffordable(
                     parentScreen.getMenu().getAvailableVendorCurrency(),
                     carried.getCount());
         Component quantity = Component.literal(String.valueOf(quote.quanity())).withColor(Colors.VALIDATION_SUCCESS);
         if (tradeItem.price() <= 0)
            return Component.translatable("menu.trading.tooltip.sell_stack_for_nothing", quantity);

         int carriedStackValue = quote.quanity() / tradeItem.quantityPerTrade() * tradeItem.price();
         return Component
               .translatable("menu.trading.tooltip.sell_stack_for", quantity, coinsTransaction(carriedStackValue))
               .withColor(Colors.TEXT_LIGHT_MUTED)
               .withStyle(ChatFormatting.ITALIC);
      }

      return coinsTransaction(tradeItem.price());
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
