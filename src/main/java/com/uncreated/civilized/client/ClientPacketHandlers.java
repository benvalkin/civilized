package com.uncreated.civilized.client;

import com.uncreated.civilized.core.notifications.Severity;
import com.uncreated.civilized.networking.packets.NotificationToast;
import com.uncreated.civilized.networking.packets.ProductionBillPreview;
import com.uncreated.civilized.networking.packets.SettlementAccessLevelDenied;
import com.uncreated.civilized.networking.packets.TradeSlotUpdated;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.EditProductionBillTab;
import com.uncreated.civilized.ui.menu.building.townhall.tabs.ManagePermissionsTab;

import com.uncreated.civilized.ui.menu.trading.TradingMenuScreen;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handlers for packets that reach into the client's screens. We have to have a client-only class to avoid crashing
 * dedicated servers since these handlers commonly reference client-only types.
 */
public final class ClientPacketHandlers {

   private ClientPacketHandlers() {
   }

   public static void receiveSettlementAccessLevelDenied(SettlementAccessLevelDenied packet, IPayloadContext context) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof ABuildingMenuScreen screen
            && screen.getCurrentTab() instanceof ManagePermissionsTab permissionsTab)
         permissionsTab.showError(packet.reason());
      // the player closed the screen or left the tab while the server was answering
      else if (minecraft.player != null)
         minecraft.player.displayClientMessage(packet.reason(), true);
   }

   /** One for every notification, so that a new one is shown alongside any still on screen rather than replacing it. */
   private static final SystemToast.SystemToastId NOTIFICATION_TOAST_ID = new SystemToast.SystemToastId();

   public static void receiveNotificationToast(NotificationToast packet, IPayloadContext context) {
      Minecraft minecraft = Minecraft.getInstance();
      Component headline = packet.headline().copy().withColor(severityColor(packet.severity()));
      // multiline, since details are usually too long for a single-line toast
      minecraft.getToastManager()
            .addToast(SystemToast.multiline(minecraft, NOTIFICATION_TOAST_ID, headline, packet.detail()));
   }

   private static int severityColor(Severity severity) {
      return switch (severity) {
      case POSITIVE -> Colors.VALIDATION_SUCCESS;
      case INFO -> Colors.TEXT_LIGHT_MUTED;
      case MINOR -> Colors.COIN;
      case MAJOR, CRITICAL -> Colors.VALIDATION_ERROR;
      };
   }

   public static void receiveProductionBillPreview(ProductionBillPreview packet, IPayloadContext context) {
      // the player may have left the edit tab or closed the screen while the server was answering
      if (Minecraft.getInstance().screen instanceof ABuildingMenuScreen screen
            && screen.getCurrentTab() instanceof EditProductionBillTab editTab)
         editTab.receivePreview(packet.sequence(), packet.resultItem(), packet.recipeAllowed());
   }

   public static void receiveTradeSlotUpdated(TradeSlotUpdated packet, IPayloadContext context) {
      if (Minecraft.getInstance().screen instanceof TradingMenuScreen screen)
         screen.receiveTradeSlotUpdated(packet.sequence(), packet.slot(), packet.newStock(), packet.newAvailableVendorCurrency());
   }
}
