package com.uncreated.civilized.client;

import com.uncreated.civilized.client.renderer.BuildingBoundsDragTool;
import com.uncreated.civilized.client.toast.SettlementNotificationToast;
import com.uncreated.civilized.networking.packets.BuildingPlacementRejected;
import com.uncreated.civilized.networking.packets.NotificationToast;
import com.uncreated.civilized.networking.packets.OpenEstablishBuildingScreen;
import com.uncreated.civilized.networking.packets.ProductionBillPreview;
import com.uncreated.civilized.networking.packets.RequirementsChecked;
import com.uncreated.civilized.networking.packets.SettlementAccessLevelDenied;
import com.uncreated.civilized.networking.packets.TradeSlotUpdated;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.EstablishBuildingScreen;
import com.uncreated.civilized.ui.menu.building.IRequirementsCheckListener;
import com.uncreated.civilized.ui.menu.building.residence.artisan.EditProductionBillTab;
import com.uncreated.civilized.ui.menu.building.townhall.tabs.ManagePermissionsTab;
import com.uncreated.civilized.ui.menu.trading.TradingMenuScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
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

   public static void receiveNotificationToast(NotificationToast packet, IPayloadContext context) {
      Minecraft.getInstance()
            .getToastManager()
            .addToast(
                  new SettlementNotificationToast(
                        packet.headline(),
                        packet.detail(),
                        packet.severity(),
                        packet.icon()));
   }

   public static void receiveProductionBillPreview(ProductionBillPreview packet, IPayloadContext context) {
      // the player may have left the edit tab or closed the screen while the server was answering
      if (Minecraft.getInstance().screen instanceof ABuildingMenuScreen screen
            && screen.getCurrentTab() instanceof EditProductionBillTab editTab)
         editTab.receivePreview(packet.sequence(), packet.resultItem(), packet.recipeAllowed());
   }

   public static void receiveBuildingPlacementRejected(BuildingPlacementRejected packet, IPayloadContext context) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null)
         player.displayClientMessage(packet.reason(), true);

      BuildingBoundsDragTool.resetDragging();
   }

   public static void openEstablishBuildingScreen(OpenEstablishBuildingScreen packet, IPayloadContext context) {
      Minecraft.getInstance()
            .setScreen(new EstablishBuildingScreen(packet.buildingType(), packet.bounds(), packet.requirements()));
   }

   public static void receiveRequirementsChecked(RequirementsChecked packet, IPayloadContext context) {
      // this response is intended for both standalone screens and also specialized building menu tabs
      Screen screen = Minecraft.getInstance().screen;
      if (screen instanceof IRequirementsCheckListener listener)
         listener.receiveRequirementsChecked(packet.requestId(), packet.results());
      else if (screen instanceof ABuildingMenuScreen buildingScreen
            && buildingScreen.getCurrentTab() instanceof IRequirementsCheckListener listener)
         listener.receiveRequirementsChecked(packet.requestId(), packet.results());
   }

   public static void receiveTradeSlotUpdated(TradeSlotUpdated packet, IPayloadContext context) {
      if (Minecraft.getInstance().screen instanceof TradingMenuScreen screen)
         screen.receiveTradeSlotUpdated(
               packet.sequence(),
               packet.slot(),
               packet.newStock(),
               packet.newAvailableVendorCurrency());
   }
}
