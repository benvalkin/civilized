package com.uncreated.civilized.core.recruitment;

import static net.minecraft.network.chat.Component.translatable;

import java.time.Duration;
import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.core.notifications.Receiver;
import com.uncreated.civilized.core.notifications.Severity;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class WillingToJoinNotification {

   private static final String TYPE = "visitor_willing_to_join";

   private WillingToJoinNotification() {
   }

   /**
    * @return whether the notification was sent
    */
   public static boolean trySendNotification(VillagerInfo visitor, boolean alreadySent) {
      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(visitor.getVisitingSettlementId());
      if (settlement.isEmpty())
         return false;

      Optional<Building> notificationPoi = findNotificationPoi(visitor);
      if (notificationPoi.isEmpty())
         return false;

      Component notificationPoiTranslation = notificationPoi.get().getBuildingType().translation();
      boolean canJoin = canJoin(visitor, settlement.get());
      if (canJoin && !alreadySent)
         NotificationService.INSTANCE.sendNotification(create(visitor, settlement.get(), notificationPoiTranslation));
      else if (!canJoin && alreadySent)
         NotificationService.INSTANCE.resolveNotification(create(visitor, settlement.get(), notificationPoiTranslation));
      return canJoin;
   }

   /**
    * For when the visitor joins or leaves. Must be called while they're still visiting the settlement (needs the
    * corresponding settlement instance in order to resolve the notification).
    */
   public static void withdrawNotification(VillagerInfo visitor) {
      // notifications are resolved by their key, which doesn't include where the visitor is
      ServerSettlementsStore.INSTANCE.find(visitor.getVisitingSettlementId())
            .ifPresent(
                  settlement -> NotificationService.INSTANCE
                        .resolveNotification(create(visitor, settlement, Component.empty())));
   }

   private static Optional<Building> findNotificationPoi(VillagerInfo visitor) {
      Optional<Building> stayingAt = ServerBuildingsStore.INSTANCE.find(visitor.getHomeBuildingId());
      if (stayingAt.isPresent())
         return stayingAt;

      Optional<Building> townSquare = ServerBuildingsStore.INSTANCE.findTownSquare(visitor.getVisitingSettlementId());
      if (townSquare.isPresent())
         return townSquare;

      return ServerBuildingsStore.INSTANCE.findTownHall(visitor.getVisitingSettlementId());
   }

   private static boolean canJoin(VillagerInfo visitor, Settlement settlement) {
      return Impression
            .compute(visitor, settlement.getSettlementId(), ServerBuildingsStore.INSTANCE, ServerVillagerStore.INSTANCE)
            .isWillingToJoin();
   }

   private static Notification create(VillagerInfo visitor, Settlement settlement, Component location) {
      return Notification.builder()
            .type(TYPE)
            .settlementId(settlement.getSettlementId())
            .villagerId(visitor.getVillagerId())
            .receiver(Receiver.ADMINISTRATION)
            .severity(Severity.POSITIVE)
            .headline(translatable("notification.visitor.willing_to_join"))
            .detail(
                  translatable(
                        "notification.visitor.willing_to_join.detail",
                        visitor.getFirstName(),
                        visitor.getNpcRole().translation(visitor.getGender()),
                        settlement.displayNameTranslation(),
                        location))
            .expireAfter(Duration.ofMinutes(20))
            .icon(new ItemStack(Items.BELL))
            .build();
   }
}
