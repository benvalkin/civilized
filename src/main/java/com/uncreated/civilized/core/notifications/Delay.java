package com.uncreated.civilized.core.notifications;

import java.time.Duration;
import java.util.function.Predicate;

public record Delay(Duration deliverAfter, Predicate<DelayedNotification> stillRelevant) {
}
