package com.uncreated.civilized.core.building;

import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.networking.packets.ShowBuildingScreen;
import com.uncreated.civilized.ui.menu.building.BuildingMenu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Opens a building's screen for a player.
 */
public class BuildingScreenOpener {

   public static void open(ServerPlayer player, Building building) {

      CompoundTag additionalData = new CompoundTag();
      building.getState().serverAddToBuildingScreenContext(additionalData, player.serverLevel());

      if (!building.getBuildingType().usesBuildingMenu()) {
         PacketDistributor.sendToPlayer(player, new ShowBuildingScreen(building.getBuildingId(), additionalData));
         return;
      }

      // Opening the menu done next tick.
      // The reason for this is that closing a menu replaces the player's open menu with their
      // inventory afterwards, which would throw this one away if it was opened while a menu was being closed
      player.server.execute(() -> openBuildingMenu(player, building, additionalData));
   }

   private static void openBuildingMenu(ServerPlayer player, Building building, CompoundTag additionalData) {
      player.openMenu(new MenuProvider() {

         @Override
         public Component getDisplayName() {
            return building.getBuildingType().translationDark();
         }

         @Override
         public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new BuildingMenu(
                  containerId,
                  inventory,
                  building,
                  ServerSettlementsStore.INSTANCE.get(building.getSettlementId()),
                  additionalData);
         }
      }, buffer -> writeMenuData(buffer, building, additionalData));
   }

   private static void writeMenuData(RegistryFriendlyByteBuf buffer, Building building, CompoundTag additionalData) {
      buffer.writeUUID(building.getBuildingId());
      buffer.writeUUID(building.getSettlementId());
      buffer.writeNbt(additionalData);
   }
}
