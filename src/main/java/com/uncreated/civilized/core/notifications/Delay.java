package com.uncreated.civilized.core.notifications;

import java.time.Duration;
import java.util.function.Predicate;

public record Delay(Duration deliverAfter, Predicate<DelayedNotification> stillRelevant) {

   public static Delay of(Duration deliverAfter, Predicate<DelayedNotification> stillRelevant) {
      return new Delay(deliverAfter, stillRelevant);
    }
}
