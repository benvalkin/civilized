package com.uncreated.civilized.util;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class CooldownTracker<T> {

   private final Map<T, Long> cooldowns;

   public CooldownTracker() {
      cooldowns = new HashMap<>();
   }

   public boolean hasCooldown(T key, long currentTicks) {
      @Nullable
      Long cooldownStartedTicks = cooldowns.get(key);
      if (cooldownStartedTicks == null)
         return false;

      if (currentTicks >= cooldownStartedTicks) {
         cooldowns.remove(key);
         return false;
      }

      return true;
   }

   public void startCooldown(T key, Duration duration, long currentTicks) {
      long durationTicks = duration.toSeconds() * 20;
      if (durationTicks == 0)
         return;

      cooldowns.put(key, currentTicks + durationTicks);
   }

   public void resetCooldown(T key) {
      cooldowns.remove(key);
   }
}
