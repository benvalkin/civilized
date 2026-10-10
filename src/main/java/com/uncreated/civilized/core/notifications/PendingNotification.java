package com.uncreated.civilized.core.notifications;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

@Accessors(fluent = true)
@Getter
public class PendingNotification {

   private static final String FIELD_NOTIFICATION = "notification";
   private static final String FIELD_CREATED_EPOCH_MILLIS = "created_epoch_millis";
   private static final String FIELD_CREATED_GAME_TIME = "created_game_time";
   private static final String FIELD_DELIVERED = "delivered";
   private static final String FIELD_RESOLVED = "resolved";

   @Setter
   private Notification notification;
   private final OffsetDateTime created;
   private final long createdGameTime;
   /**
    * Set to {@code true} as soon as any relevant player (only one required) has seen the notification
    */
   @Setter
   private boolean delivered;
   @Setter
   private boolean resolved;

   public PendingNotification(Notification notification, long currentGameTime) {
      this(notification, OffsetDateTime.now(), currentGameTime, false, false);
   }

   private PendingNotification(
         Notification notification,
         OffsetDateTime created,
         long createdGameTime,
         boolean delivered,
         boolean resolved) {
      this.notification = notification;
      this.created = created;
      this.createdGameTime = createdGameTime;
      this.delivered = delivered;
      this.resolved = resolved;
   }

   public boolean isExpired(long gameTime) {
      long elapsedSeconds = (gameTime - createdGameTime) / 20;
      return elapsedSeconds >= notification.expireAfter().toSeconds();
   }

   public CompoundTag toNbt(HolderLookup.Provider registries) {
      CompoundTag tag = new CompoundTag();
      tag.put(FIELD_NOTIFICATION, notification.toNbt(registries));
      tag.putLong(FIELD_CREATED_EPOCH_MILLIS, created.toInstant().toEpochMilli());
      tag.putLong(FIELD_CREATED_GAME_TIME, createdGameTime);
      tag.putBoolean(FIELD_DELIVERED, delivered);
      tag.putBoolean(FIELD_RESOLVED, resolved);
      return tag;
   }

   public static Optional<PendingNotification> fromNbt(CompoundTag tag, HolderLookup.Provider registries) {
      return Notification.fromNbt(tag.getCompound(FIELD_NOTIFICATION), registries)
            .map(
                  notification -> new PendingNotification(
                        notification,
                        OffsetDateTime.ofInstant(
                              Instant.ofEpochMilli(tag.getLong(FIELD_CREATED_EPOCH_MILLIS)),
                              ZoneId.systemDefault()),
                        tag.getLong(FIELD_CREATED_GAME_TIME),
                        tag.getBoolean(FIELD_DELIVERED),
                        tag.getBoolean(FIELD_RESOLVED)));
   }
}
