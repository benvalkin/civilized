package com.uncreated.civilized.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.uncreated.civilized.core.trading.TradingScreenOpener;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

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
      TradingScreenOpener.open(source.getPlayerOrException());
      return 1;
   }
}
