package com.uncreated.civilized.core.notifications;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class DelayedNotification {
   @Setter
   private Notification notification;
   private final Delay delay;
   private final long createdGameTime;

   public DelayedNotification(Notification notification, Delay delay, long createdGameTime) {
      this.createdGameTime = createdGameTime;
      this.delay = delay;
      this.notification = notification;
   }

   public boolean isTimeToDeliver(long gameTime) {
      long elapsedSeconds = (gameTime - createdGameTime()) / 20;
      return elapsedSeconds >= delay.deliverAfter().toSeconds();
   }

   public boolean isStillRelevant() {
      return delay.stillRelevant().test(this);
   }
}
