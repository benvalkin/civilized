package com.uncreated.civilized.core.trading;

import java.util.List;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.menu.trading.TradingMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class TradingScreenOpener {

   public static void open(
         ServerPlayer player,
         CivilizedVillager vendor,
         List<TradeItem> tradeItems,
         CurrencyStock vendorCurrency) {

      // Opening the menu done next tick.
      // The reason for this is that closing a menu replaces the player's open menu with their
      // inventory afterwards, which would throw this one away if it was opened while a menu was being closed
      player.server.execute(() -> openTradingMenu(player, vendor, tradeItems, vendorCurrency));
   }

   private static void openTradingMenu(
         ServerPlayer player,
         CivilizedVillager vendor,
         List<TradeItem> tradeItems,
         CurrencyStock vendorCurrency) {

      // note: vendorCurrency is deliberately passed by reference into the menu, so that when trading via the menu and
      // increasing/reducing the merchants funds, the new int value is already reflected in the merchant entity instance
      // and ready to be persisted to level storage when the game saves.

      player.openMenu(new MenuProvider() {

         @Override
         public Component getDisplayName() {
            return Component.translatable("menu.trading_screen");
         }

         @Override
         public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new TradingMenu(containerId, inventory, vendor, vendorCurrency, tradeItems, inventory);
         }
      }, buffer -> writeMenuData(buffer, vendorCurrency.availableCurrency(), tradeItems));
   }

   /** Has to be written in the same order that {@link TradingMenu}'s client constructor reads it. */
   private static void writeMenuData(
         RegistryFriendlyByteBuf buffer,
         int availableVendorCurrency,
         List<TradeItem> tradeItems) {
      buffer.writeInt(availableVendorCurrency);
      TradingMenu.TRADES_STREAM_CODEC.encode(buffer, tradeItems);
   }
}
