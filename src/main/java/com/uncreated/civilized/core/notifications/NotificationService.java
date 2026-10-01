package com.uncreated.civilized.core.notifications;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.permission.PlayerPermission;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.networking.packets.NotificationToast;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

public class NotificationService extends SavedData {

   public static NotificationService INSTANCE;

   public static final String STORAGE_FILE_NAME = "civilized_notifications";
   private static final String FIELD_NOTIFICATIONS = "notifications";

   private static final int TICK_INTERVAL = 20 * 10;

   private final Map<String, PendingNotification> notifications = new HashMap<>();
   private @Nullable MinecraftServer server;

   public static void loadServer(MinecraftServer server) {
      INSTANCE =
            server.overworld()
                  .getDataStorage()
                  .computeIfAbsent(
                        new SavedData.Factory<>(NotificationService::new, NotificationService::load),
                        STORAGE_FILE_NAME);
      INSTANCE.server = server;
   }

   public void sendNotification(Notification notification) {
      PendingNotification existing = notifications.get(notification.key());
      if (existing != null) {
         // do not resend similar notifications
         existing.resolved(false); // unresolve in case it was recently resolved
         existing.notification(notification); // update the existing's notification in case it's different
         return;
      }
      PendingNotification pendingNotification = new PendingNotification(notification, server.overworld().getGameTime());
      notifications.put(notification.key(), pendingNotification);
      setDirty();
      if (pendingNotification.isTimeToDeliver(pendingNotification.createdGameTime()))
         pushNotificationToRelevantPlayers(pendingNotification);
   }

   public void tick(long gameTime) {
      if (gameTime % TICK_INTERVAL != 0)
         return;

      List<String> keysToRemove = new LinkedList<>();
      for (PendingNotification pending : notifications.values()) {
         if (pending.resolved() || pending.isExpired(gameTime)) {
            keysToRemove.add(pending.notification().key());
            continue;
         }

         if (!pending.delivered() && pending.isTimeToDeliver(gameTime))
            pushNotificationToRelevantPlayers(pending);
      }

      for (String keyToRemove : keysToRemove)
         notifications.remove(keyToRemove);

      if (!keysToRemove.isEmpty())
         setDirty();
   }

   public void resolveNotification(@Nullable Notification notification) {
      if (notification == null)
         return;

      PendingNotification removed = notifications.remove(notification.key());
      if (removed == null)
         return;

      removed.resolved(true);
      setDirty();
   }

   private void pushNotificationToRelevantPlayers(PendingNotification pending) {
      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(pending.notification().settlementId());

      boolean sent = false;
      for (PlayerPermission playerPermission : permissions.entries()) {
         if (!isCorrectReceiverGroup(pending.notification(), playerPermission))
            continue;

         ServerPlayer player = server.getPlayerList().getPlayer(playerPermission.playerId());
         if (player == null)
            continue; // offline, so it's tried again later

         PacketDistributor.sendToPlayer(player, NotificationToast.of(pending.notification()));
         sent = true;
      }

      if (sent) {
         pending.delivered(true);
         setDirty();
      }
   }

   private static boolean isCorrectReceiverGroup(Notification notification, PlayerPermission playerPermission) {
      List<Receiver> playerReceiverGroup = Receiver.fromAccessLevel(playerPermission.accessLevel());
      return playerReceiverGroup.contains(notification.receiver());
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
      ListTag list = new ListTag();
      for (PendingNotification pending : notifications.values())
         list.add(pending.toNbt(registries));

      tag.put(FIELD_NOTIFICATIONS, list);
      return tag;
   }

   private static NotificationService load(CompoundTag tag, HolderLookup.Provider registries) {
      NotificationService service = new NotificationService();
      for (Tag t : tag.getList(FIELD_NOTIFICATIONS, Tag.TAG_COMPOUND)) {
         if (t instanceof CompoundTag pendingTag)
            PendingNotification.fromNbt(pendingTag, registries)
                  .ifPresent(pending -> service.notifications.put(pending.notification().key(), pending));
      }
      return service;
   }
}
