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
      Optional<Building> stayingAt = ServerBuildingsStore.INSTANCE.find(visitor.getHomeBuildingId());
      if (stayingAt.isEmpty())
         return false;

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(stayingAt.get().getSettlementId());
      if (settlement.isEmpty())
         return false;

      boolean canJoin = canJoin(visitor, settlement.get());
      if (canJoin && !alreadySent)
         NotificationService.INSTANCE.sendNotification(create(visitor, settlement.get(), stayingAt.get()));
      else if (!canJoin && alreadySent)
         NotificationService.INSTANCE.resolveNotification(create(visitor, settlement.get(), stayingAt.get()));
      return canJoin;
   }

   /**
    * For when the visitor joins or leaves. Must be called while they're still staying at the visited building (needs
    * the corresponding settlement instance in order to resolve the notification).
    */
   public static void withdrawNotification(VillagerInfo visitor) {
      Optional<Building> stayingAt = ServerBuildingsStore.INSTANCE.find(visitor.getHomeBuildingId());
      if (stayingAt.isEmpty())
         return;

      ServerSettlementsStore.INSTANCE.find(stayingAt.get().getSettlementId())
            .ifPresent(
                  settlement -> NotificationService.INSTANCE
                        .resolveNotification(create(visitor, settlement, stayingAt.get())));
   }

   private static boolean canJoin(VillagerInfo visitor, Settlement settlement) {
      return Impression
            .compute(
                  visitor,
                  settlement.getSettlementId(),
                  ServerBuildingsStore.INSTANCE,
                  ServerVillagerStore.INSTANCE)
            .isWillingToJoin();
   }

   private static Notification create(VillagerInfo visitor, Settlement settlement, Building stayingAt) {
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
                        stayingAt.getBuildingType().translation()))
            .expireAfter(Duration.ofMinutes(20))
            .icon(new ItemStack(Items.BELL))
            .build();
   }
}
