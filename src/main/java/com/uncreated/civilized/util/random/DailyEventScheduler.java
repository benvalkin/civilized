package com.uncreated.civilized.util.random;

import java.util.PriorityQueue;
import java.util.Random;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerLevel;

public class DailyEventScheduler {

   private final Random random;

   private final int eventsRollsPerDay;
   private final int eventWindowStartTime;
   private final int eventWindowCutoffTime;
   private final float eventRollChance;

   private final PriorityQueue<Long> eventQueue;
   private boolean needNewEvents;

   private final Logger LOGGER = LogUtils.getLogger();

   public DailyEventScheduler(
         int eventsRollsPerDay,
         int eventWindowStartTime,
         int eventWindowCutoffTime,
         float eventRollChance) {
      this.random = new Random();
      this.eventsRollsPerDay = eventsRollsPerDay;
      this.eventWindowStartTime = eventWindowStartTime;
      this.eventWindowCutoffTime = eventWindowCutoffTime;
      this.eventRollChance = eventRollChance;
      // the capacity must be at least 1, even when there are no events to roll
      this.eventQueue = new PriorityQueue<>(Math.max(1, this.eventsRollsPerDay));
      this.needNewEvents = true;
   }

   public void tick(ServerLevel level, long gameTime, Runnable onEventTriggered) {

      long todayTime = level.getDayTime() % 24000;
      if (todayTime >= eventWindowStartTime && todayTime < eventWindowCutoffTime) {
         if (needNewEvents) {
            eventQueue.clear();
            for (int i = 0; i < eventsRollsPerDay; i++)
               eventQueue.add(todayTime + random.nextLong(eventWindowCutoffTime - todayTime));

            LOGGER.debug(
                  "Queued {} new events at the respective times: {}",
                  eventQueue.size(),
                  String.join(", ", eventQueue.stream().map(String::valueOf).toList()));

            needNewEvents = false;
         }
         if (!eventQueue.isEmpty() && todayTime >= eventQueue.peek()) {
            Long eventDayTime = eventQueue.remove();

            LOGGER.debug("Event (which was scheduled for {}) was triggered at {}", eventDayTime, todayTime);
            // event has been triggered

            if (random.nextFloat() <= eventRollChance)
               onEventTriggered.run();
         }
      } else if (!needNewEvents) {
         // event window closes at the end of the day
         // mark that we need to queue new events when the day is done
         needNewEvents = true;
         eventQueue.clear();
         LOGGER.debug("Event window has closed for today.");
      }
   }

}
