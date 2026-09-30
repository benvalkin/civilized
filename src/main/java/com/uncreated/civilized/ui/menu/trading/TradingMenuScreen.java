package com.uncreated.civilized.ui.menu.trading;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.core.trading.TradeDirection;
import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.networking.packets.BuyItem;
import com.uncreated.civilized.networking.packets.SellItem;
import com.uncreated.civilized.ui.components.IRefreshableUI;
import com.uncreated.civilized.ui.components.SlotFrameRenderer;
import com.uncreated.civilized.ui.components.widget.EmptyTradeSlotWidget;
import com.uncreated.civilized.ui.components.widget.ITradeSlot;
import com.uncreated.civilized.ui.components.widget.TradeSlotWidget;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class TradingMenuScreen extends AbstractContainerScreen<TradingMenu> implements IRefreshableUI {

   private static final ResourceLocation MENU_TEXTURE =
         ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "textures/gui/building_menu.png");

   private static final int CONTENT_MARGIN_X = 25;
   private static final int CONTENT_MARGIN_Y = 20;
   private static final int TRADE_DIRECTION_BUTTON_WIDTH = 60;
   private static final int TRADE_DIRECTION_BUTTON_HEIGHT = 16;

   private static final int TRADE_SLOT_COLUMNS = 9;
   private static final int TRADE_SLOT_ROWS = 3;
   private static final int TRADE_SLOT_SPACING = 18;

   private List<ITradeSlot> tradeSlots;
   // kept across init(), which runs again whenever the window is resized
   @Getter
   private TradeDirection tradeDirection = TradeDirection.BUY;

   public TradingMenuScreen(TradingMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 340;
      this.imageHeight = 200;
      this.titleLabelX = 30;
      this.titleLabelY = 2;
      this.inventoryLabelX = 90;
      this.inventoryLabelY = this.imageHeight - 107;
   }

   private int sequence = 0;

   private void getOnClicked(TradeSlotWidget tradeSlotWidget) {

      // avoid using Minecraft.getInstance.getWindow bc it doesn't check right shift
      boolean isShiftKeyPressed = Screen.hasShiftDown();

      ItemStack carried = getMenu().getCarried();

      TradeItem tradeItem = tradeSlotWidget.getTradeItem();
      int slot = tradeSlotWidget.getSlot();

      if (tradeItem.item().isEmpty())
         return; // cannot click on empty trade slots

      if (tradeDirection == TradeDirection.BUY) {

         if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, tradeItem.item()))
            return; // you can only buy an item from a slot if your hand is empty or you are carrying the same item

         // one trade at a time, or as many as fit in a stack with shift. The server rounds down to whole trades
         int quantityToBuy;
         if (!isShiftKeyPressed)
            quantityToBuy = tradeItem.quantityPerTrade();
         else
            quantityToBuy = Math.min(tradeItem.stock(), tradeItem.item().getMaxStackSize());

         sequence++;
         PacketDistributor.sendToServer(new BuyItem(sequence, slot, quantityToBuy));
      } else if (tradeDirection == TradeDirection.SELL) {
         if (carried.isEmpty())
            return; // cannot sell an empty item

         if (!ItemStack.isSameItemSameComponents(carried, tradeItem.item()))
            return; // trying to click with the wrong type of item

         sequence++;
         PacketDistributor.sendToServer(new SellItem(sequence, slot));
      }
   }

   @Override
   protected void init() {
      super.init();

      List<TradeItem> tradeItems = menu.getTradeItems();
      tradeSlots = new ArrayList<>();

      // a full grid is always shown, with empty slots after the trades, growing by a row if there are more trades
      int rows = Math.max(TRADE_SLOT_ROWS, Math.ceilDiv(tradeItems.size(), TRADE_SLOT_COLUMNS));

      int gridWidth = (TRADE_SLOT_COLUMNS - 1) * TRADE_SLOT_SPACING + TradeSlotWidget.SIZE;
      int gridX = leftPos + (imageWidth - gridWidth) / 2;
      int gridY = topPos + CONTENT_MARGIN_Y;

      for (int i = 0; i < rows * TRADE_SLOT_COLUMNS; i++) {
         int x = gridX + (i % TRADE_SLOT_COLUMNS) * TRADE_SLOT_SPACING;
         int y = gridY + (i / TRADE_SLOT_COLUMNS) * TRADE_SLOT_SPACING;

         if (i < tradeItems.size())
            addTradeSlot(new TradeSlotWidget(this, x, y, i, tradeItems.get(i), this::getOnClicked));
         else
            addTradeSlot(new EmptyTradeSlotWidget(x, y, i));
      }

      addRenderableWidget(
            CycleButton.builder(TradeDirection::translation)
                  .withValues(TradeDirection.values())
                  .withInitialValue(tradeDirection)
                  .withTooltip(direction -> Tooltip.create(direction.description()))
                  .displayOnlyValue()
                  .create(
                        leftPos + CONTENT_MARGIN_X,
                        topPos + CONTENT_MARGIN_Y,
                        TRADE_DIRECTION_BUTTON_WIDTH,
                        TRADE_DIRECTION_BUTTON_HEIGHT,
                        Component.empty(),
                        (button, direction) -> tradeDirection = direction));
   }

   private <T extends AbstractWidget & ITradeSlot> void addTradeSlot(T tradeSlot) {
      tradeSlots.add(tradeSlot);
      addRenderableWidget(tradeSlot);
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(
            RenderType::guiTextured,
            MENU_TEXTURE,
            leftPos,
            topPos,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            384,
            384);

      // the background texture has no slots painted on it, since the inventory only shows on some tabs
      for (Slot slot : menu.slots) {
         if (slot.isActive())
            SlotFrameRenderer.render(graphics, leftPos + slot.x, topPos + slot.y);
      }
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      // graphics.drawString(font, title, titleLabelX, titleLabelY, Colors.MENU_TEXT_DARK, false);

      // the inventory heading would otherwise sit on its own above nothing
      graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Colors.MENU_TEXT_DARK, false);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);

      renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   public void refresh() {

   }

   public void receiveTradeSlotUpdated(int sequence, int slot, int newStock, int newAvailableVendorCurrency) {

      if (sequence != this.sequence)
         return;

      if (slot < 0 || slot >= tradeSlots.size())
         return;

      TradeItem tradeItem = tradeSlots.get(slot).getTradeItem();
      if (tradeItem != null)
         tradeItem.stock(newStock);
      menu.setAvailableVendorCurrency(newAvailableVendorCurrency);
   }
}
