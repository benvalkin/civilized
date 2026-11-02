package com.uncreated.civilized.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.uncreated.civilized.entity.pathfinding.PathfindingBudget;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Displays pathfinding metrics of all currently loaded villagers from the last few seconds (prints them to chat). */
public class ShowPathfindingMetrics {

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("showpathfindingmetrics")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> showStats(context.getSource())));
   }

   private static int showStats(CommandSourceStack source) {
      PathfindingBudget.INSTANCE.tryRollOverCurrentWindow(source.getLevel().getGameTime());
      PathfindingBudget.Metrics stats = PathfindingBudget.INSTANCE.getLastWindow();
      double seconds = PathfindingBudget.STATS_WINDOW_TICKS / 20.0;

      String message =
            String.format(
                  "Travel path searches over the last %.0fs: %.1f/s (%d failed, %d deferred), avg %.3fms, max %.3fms, total %.2fms/s",
                  seconds,
                  stats.getSearches() / seconds,
                  stats.getFailedSearches(),
                  stats.getDeferredSearches(),
                  stats.averageMillis(),
                  stats.maxMillis(),
                  stats.getTotalNanos() / 1_000_000.0 / seconds);
      source.sendSuccess(() -> Component.literal(message), false);
      return stats.getSearches();
   }
}
