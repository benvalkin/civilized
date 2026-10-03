package com.uncreated.civilized.commands;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.uncreated.civilized.core.trading.CurrencyStock;
import com.uncreated.civilized.core.trading.TradeItem;
import com.uncreated.civilized.core.trading.TradingScreenOpener;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Dev command that opens the trading menu with its placeholder trades, e.g. {@code /tradingmenu}. */
public class OpenTradingMenu {

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("tradingmenu")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> open(context.getSource())));
   }

   private static int open(CommandSourceStack source) throws CommandSyntaxException {
      // fails with the usual "only players may use this" message when run from the console

      int availableVendorCurrency = 200;

      List<TradeItem> tradeItems = new ArrayList<>();
      // placeholder slots
      tradeItems.add(new TradeItem(new ItemStack(Items.LEATHER), 7, 30));
      tradeItems.add(new TradeItem(new ItemStack(Items.APPLE), 1, 30));
      tradeItems.add(new TradeItem(new ItemStack(Items.EMERALD), 24, 3));
      tradeItems.add(new TradeItem(new ItemStack(Items.COBBLESTONE), 1, 256, 2));
      tradeItems.add(new TradeItem(new ItemStack(Items.PINK_TULIP), 24, 12));

      TradingScreenOpener
            .open(source.getPlayerOrException(), null, tradeItems, new CurrencyStock(availableVendorCurrency));
      return 1;
   }
}
