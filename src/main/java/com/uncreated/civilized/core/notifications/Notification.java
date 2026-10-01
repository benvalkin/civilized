package com.uncreated.civilized.core.notifications;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.villagerinfo.VillagerInfo;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;

@Accessors(fluent = true)
@Getter
public class Notification {

   private static final String FIELD_TYPE = "type";
   private static final String FIELD_SETTLEMENT_ID = "settlement_id";
   private static final String FIELD_VILLAGER_ID = "villager_id";
   private static final String FIELD_RECEIVER = "receiver";
   private static final String FIELD_SEVERITY = "severity";
   private static final String FIELD_EXPIRE_AFTER_SECONDS = "expire_after_seconds";
   private static final String FIELD_HEADLINE = "headline";
   private static final String FIELD_DETAIL = "detail";

   private final String type;
   private final UUID settlementId;
   private final @Nullable UUID villagerId;
   private final Receiver receiver;
   private final Severity severity;
   private final Duration expireAfter;
   private final @Nullable Duration deliverAfter;
   private final Component headline;
   private final Component detail;

   @Builder
   private Notification(
         String type,
         UUID settlementId,
         @Nullable UUID villagerId,
         Receiver receiver,
         Severity severity,
         Duration expireAfter,
         Duration deliverAfter,
         Component headline,
         Component detail) {
      this.type = type;
      this.settlementId = settlementId;
      this.villagerId = villagerId;
      this.receiver = receiver;
      this.severity = severity;
      this.expireAfter = expireAfter;
      this.deliverAfter = deliverAfter;
      this.headline = headline;
      this.detail = detail;
   }

   public String key() {
      String key = type + "_" + settlementId;
      if (villagerId != null)
         key += "_" + villagerId;
      return key;
   }

   public static Notification.NotificationBuilder missingTool(String type, VillagerInfo info, Item item) {

      if (info.getSettlementId() == null)
         throw new IllegalArgumentException("Villager's settlementId must not be null");

      Component headline =
            Component.translatable(
                  "notification.worker.missing_tool.generic",
                  info.getOccupation().translation(),
                  item.getName());

      Component detail =
            Component.translatable(
                  "notification.worker.missing_tool.generic.detail",
                  info.getFirstName(),
                  info.getOccupation().translation(),
                  item.getName());

      return Notification.builder()
            .type(type)
            .settlementId(info.getSettlementId())
            .villagerId(info.getVillagerId())
            .receiver(Receiver.ADMINISTRATION)
            .severity(Severity.MINOR)
            .headline(headline)
            // TODO: make the exipiry value longer - it's been reduced for testing
            .expireAfter(Duration.of(30, ChronoUnit.SECONDS))
            .detail(detail);
   }

   public static Notification.NotificationBuilder missingRecipeInput(
         String type,
         UUID settlementId,
         UUID villagerId,
         Component headline,
         Component detail) {

      return Notification.builder()
            .type(type)
            .settlementId(settlementId)
            .villagerId(villagerId)
            .receiver(Receiver.ADMINISTRATION)
            .severity(Severity.MINOR)
            .headline(headline)
            .deliverAfter(Duration.of(3, ChronoUnit.MINUTES))
            .expireAfter(Duration.of(10, ChronoUnit.MINUTES))
            .detail(detail);
   }

   public CompoundTag toNbt(HolderLookup.Provider registries) {
      RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);

      CompoundTag tag = new CompoundTag();
      tag.putString(FIELD_TYPE, type);
      tag.putUUID(FIELD_SETTLEMENT_ID, settlementId);
      if (villagerId != null)
         tag.putUUID(FIELD_VILLAGER_ID, villagerId);
      tag.putString(FIELD_RECEIVER, receiver.name());
      tag.putString(FIELD_SEVERITY, severity.name());
      tag.putLong(FIELD_EXPIRE_AFTER_SECONDS, expireAfter.toSeconds());
      tag.put(FIELD_HEADLINE, ComponentSerialization.CODEC.encodeStart(ops, headline).getOrThrow());
      tag.put(FIELD_DETAIL, ComponentSerialization.CODEC.encodeStart(ops, detail).getOrThrow());
      return tag;
   }

   /** Empty if the notification can't be read, e.g. because a receiver or severity it used no longer exists. */
   public static Optional<Notification> fromNbt(CompoundTag tag, HolderLookup.Provider registries) {
      RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);

      try {
         return Optional.of(
               Notification.builder()
                     .type(tag.getString(FIELD_TYPE))
                     .settlementId(tag.getUUID(FIELD_SETTLEMENT_ID))
                     .villagerId(tag.hasUUID(FIELD_VILLAGER_ID) ? tag.getUUID(FIELD_VILLAGER_ID) : null)
                     .receiver(Receiver.valueOf(tag.getString(FIELD_RECEIVER)))
                     .severity(Severity.valueOf(tag.getString(FIELD_SEVERITY)))
                     .expireAfter(Duration.ofSeconds(tag.getLong(FIELD_EXPIRE_AFTER_SECONDS)))
                     .headline(ComponentSerialization.CODEC.parse(ops, tag.get(FIELD_HEADLINE)).getOrThrow())
                     .detail(ComponentSerialization.CODEC.parse(ops, tag.get(FIELD_DETAIL)).getOrThrow())
                     .build());
      } catch (IllegalArgumentException | IllegalStateException ex) {
         return Optional.empty();
      }
   }
}
