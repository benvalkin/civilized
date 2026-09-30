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
import com.uncreated.civilized.ui.components.widget.TradeSlotWidget;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.GuiGraphics;
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
   private ArrayList<TradeSlotWidget> tradeSlots;
   private TradeDirection tradeDirection;

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

         int quantityToBuy;
         if (!isShiftKeyPressed)
            quantityToBuy = 1;
         else if (tradeItem.stock() < tradeItem.item().getMaxStackSize())
            quantityToBuy = tradeItem.stock();
         else
            quantityToBuy = tradeItem.item().getMaxStackSize();

         sequence++;
         PacketDistributor.sendToServer(new BuyItem(sequence, slot, quantityToBuy));
      } else if (tradeDirection == TradeDirection.SELL) {
         if (carried.isEmpty())
            return; // cannot sell an empty item

         if (!ItemStack.isSameItem(carried, tradeItem.item()))
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

      int slotRow = 0;
      int slotColumn = 0;
      // todo: probably need to fix up this the slot placement
      for (int i = 0; i < tradeItems.size(); i++) {
         TradeItem tradeItem = tradeItems.get(i);
         tradeSlots.add(
               new TradeSlotWidget(
                     this,
                     leftPos + 20 + slotColumn * 18,
                     topPos + 20 + slotRow * 18,
                     i,
                     tradeItem,
                     this::getOnClicked));
         slotColumn++;
         if (slotColumn % 9 == 0) {
            slotColumn = 0;
            slotRow++;
         }
      }

      tradeSlots.forEach(this::addRenderableWidget);

      tradeDirection = TradeDirection.BUY;
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
      // commented out the below line as it might be better for GUI space not to render the the building name at the top
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

      TradeSlotWidget tradeSlotWidget = tradeSlots.get(slot);
      tradeSlotWidget.getTradeItem().stock(newStock);
      menu.setAvailableVendorCurrency(newAvailableVendorCurrency);
   }
}
