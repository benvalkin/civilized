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
import net.minecraft.world.item.ItemStack;

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
   private static final String FIELD_DELIVER_AFTER_SECONDS = "deliver_after_seconds";
   private static final String FIELD_ICON = "icon";

   private final String type;
   private final UUID settlementId;
   private final @Nullable UUID villagerId;
   private final Receiver receiver;
   private final Severity severity;
   private final Duration expireAfter;
   private final @Nullable Duration deliverAfter;
   private final Component headline;
   private final Component detail;
   private final ItemStack icon;

   @Builder
   private Notification(
         String type,
         UUID settlementId,
         @Nullable UUID villagerId,
         Receiver receiver,
         Severity severity,
         Duration expireAfter,
         @Nullable Duration deliverAfter,
         Component headline,
         Component detail,
         @Nullable ItemStack icon) {
      this.type = type;
      this.settlementId = settlementId;
      this.villagerId = villagerId;
      this.receiver = receiver;
      this.severity = severity;
      this.expireAfter = expireAfter;
      this.deliverAfter = deliverAfter;
      this.headline = headline;
      this.detail = detail;
      // copied, so that changes to the stack it was made from don't show up in the notification
      this.icon = icon == null ? ItemStack.EMPTY : icon.copy();
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

      Component headline = Component.translatable("notification.worker.missing_tool.generic", item.getName());

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
            .expireAfter(Duration.of(5, ChronoUnit.MINUTES))
            .detail(detail)
            .icon(new ItemStack(item));
   }

   public static Notification.NotificationBuilder missingIngredients(String type, VillagerInfo info, Item item) {

      if (info.getSettlementId() == null)
         throw new IllegalArgumentException("Villager's settlementId must not be null");

      Component headline = Component.translatable("notification.worker.missing_ingredients.generic", item.getName());

      Component detail =
            Component.translatable(
                  "notification.worker.missing_ingredients.generic.detail",
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
            .deliverAfter(Duration.of(3, ChronoUnit.MINUTES))
            .expireAfter(Duration.of(10, ChronoUnit.MINUTES))
            .detail(detail)
            .icon(new ItemStack(item));
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
      if (deliverAfter != null)
         tag.putLong(FIELD_DELIVER_AFTER_SECONDS, deliverAfter.toSeconds());
      if (!icon.isEmpty())
         tag.put(FIELD_ICON, ItemStack.CODEC.encodeStart(ops, icon).getOrThrow());
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
                     .deliverAfter(
                           tag.contains(FIELD_DELIVER_AFTER_SECONDS)
                                 ? Duration.ofSeconds(tag.getLong(FIELD_DELIVER_AFTER_SECONDS))
                                 : null)
                     .icon(
                           tag.contains(FIELD_ICON)
                                 ? ItemStack.CODEC.parse(ops, tag.get(FIELD_ICON)).result().orElse(ItemStack.EMPTY)
                                 : ItemStack.EMPTY)
                     .build());
      } catch (IllegalArgumentException | IllegalStateException ex) {
         return Optional.empty();
      }
   }
}
