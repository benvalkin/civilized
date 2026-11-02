package com.uncreated.civilized.entity.pathfinding;

import lombok.Getter;

/**
 * Limits how many path creation attempts villagers using
 * {@link com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask} can be made per tick across the whole
 * server. This is so that lots of villagers setting off at once (e.g. at the start of a work shift) get spread over a
 * few ticks. Also instantiates some metrics for pathfinding performance debugging.
 */
public class PathfindingBudget {

   public static final PathfindingBudget INSTANCE = new PathfindingBudget();

   private static final int MAX_SEARCHES_PER_TICK = 8;
   public static final int STATS_WINDOW_TICKS = 5 * 20;

   private long currentTick = -1;
   private int searchesThisTick;

   private long windowStart;
   private Metrics currentWindow = new Metrics();
   @Getter
   private Metrics lastWindow = new Metrics();

   public static class Metrics {
      @Getter
      private int searches;
      @Getter
      private int failedSearches;
      /** Searches that had to wait for a later tick because the budget was used up. */
      @Getter
      private int deferredSearches;
      @Getter
      private long totalNanos;
      @Getter
      private long maxNanos;

      public double averageMillis() {
         if (searches == 0)
            return 0;

         return totalNanos / (double) searches / 1_000_000;
      }

      public double maxMillis() {
         return maxNanos / 1_000_000.0;
      }
   }

   /** @return whether the caller may search for a path this tick */
   public boolean tryAcquire(long gameTime) {
      tryRollOverCurrentWindow(gameTime);
      if (gameTime != currentTick) {
         currentTick = gameTime;
         searchesThisTick = 0;
      }

      if (searchesThisTick >= MAX_SEARCHES_PER_TICK) {
         currentWindow.deferredSearches++;
         return false;
      }

      searchesThisTick++;
      return true;
   }

   public void recordSearch(long nanos, boolean failed) {
      currentWindow.searches++;
      currentWindow.totalNanos += nanos;
      currentWindow.maxNanos = Math.max(currentWindow.maxNanos, nanos);
      if (failed)
         currentWindow.failedSearches++;
   }

   /** Starts a new stats window once the current one is over. */
   public void tryRollOverCurrentWindow(long gameTime) {
      if (gameTime - windowStart < STATS_WINDOW_TICKS)
         return;

      // a window with no searches at all means nobody has been travelling for a while
      if (gameTime - windowStart < STATS_WINDOW_TICKS * 2)
         lastWindow = currentWindow;
      else
         lastWindow = new Metrics();

      currentWindow = new Metrics();
      windowStart = gameTime;
   }
}
