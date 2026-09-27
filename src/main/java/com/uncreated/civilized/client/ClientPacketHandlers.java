package com.uncreated.civilized.client;

import com.uncreated.civilized.networking.packets.ProductionBillPreview;
import com.uncreated.civilized.ui.menu.building.ABuildingMenuScreen;
import com.uncreated.civilized.ui.menu.building.residence.artisan.EditProductionBillTab;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handlers for packets that reach into the client's screens. Client-only class - Avoid loading this class on dedicated servers.
 */
public final class ClientPacketHandlers {

   private ClientPacketHandlers() {
   }

   public static void receiveProductionBillPreview(ProductionBillPreview packet, IPayloadContext context) {
      // the player may have left the edit tab or closed the screen while the server was answering
      if (Minecraft.getInstance().screen instanceof ABuildingMenuScreen screen
            && screen.getCurrentTab() instanceof EditProductionBillTab editTab)
         editTab.receivePreview(packet.sequence(), packet.resultItem(), packet.recipeAllowed());
   }
}
