package com.uncreated.civilized.core.trading;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.ui.menu.trading.TradingMenu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TradingScreenOpener {

   public static void open(ServerPlayer player) {

      // Opening the menu done next tick.
      // The reason for this is that closing a menu replaces the player's open menu with their
      // inventory afterwards, which would throw this one away if it was opened while a menu was being closed
      player.server.execute(() -> openTradingMenu(player));
   }

   private static void openTradingMenu(ServerPlayer player) {

      int availableVendorCurrency = 200;

      List<TradeItem> tradeItems = new ArrayList<>();
      // placeholder slots
      tradeItems.add(new TradeItem(new ItemStack(Items.LEATHER), 7, 30));
      tradeItems.add(new TradeItem(new ItemStack(Items.APPLE), 1, 30));
      tradeItems.add(new TradeItem(new ItemStack(Items.EMERALD), 24, 3));
      tradeItems.add(new TradeItem(new ItemStack(Items.COBBLESTONE), 1, 256, 2));
      tradeItems.add(new TradeItem(new ItemStack(Items.PINK_TULIP), 24, 12));

//      SimpleContainer vendorContainer = new SimpleContainer(27);


      player.openMenu(new MenuProvider() {

         @Override
         public Component getDisplayName() {
            return Component.translatable("menu.trading_screen");
         }

         @Override
         public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new TradingMenu(containerId, inventory, availableVendorCurrency, tradeItems, inventory);
         }
      }, buffer -> writeMenuData(buffer, availableVendorCurrency, tradeItems));
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
