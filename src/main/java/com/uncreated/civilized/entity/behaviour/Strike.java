package com.uncreated.civilized.entity.behaviour;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.core.notifications.Receiver;
import com.uncreated.civilized.core.notifications.Severity;
import com.uncreated.civilized.entity.behaviour.strike.GoToTownSquare;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Strike extends StrollWithinBuildingBounds {

   public Strike(float speedModifier) {
      super(
            BehaviourStates.STROLLING_IN_TOWN_SQUARE,
            20 * 20,
            40 * 20,
            4,
            2,
            speedModifier,
            GoToTownSquare::findLoadedTownSquare);
      stroller.setOnArrived((villager, bounds) -> {
         UUID settlementId = villager.getInfo().getSettlementId();
         if (settlementId != null) {
            Notification notification = strikeNotification(settlementId);
            NotificationService.INSTANCE.sendNotification(notification);
            // don't really need to resolve the notification, right?
         }
      });
   }

   private static Notification strikeNotification(UUID settlementId) {

      Component headline = Component.translatable("notification.villager.hunger.strike");

      Component detail = Component.translatable("notification.villager.strike.hunger.detail");

      return Notification.builder()
            .type("strike_hunger")
            .settlementId(settlementId)
            .receiver(Receiver.ADMINISTRATION)
            .severity(Severity.MAJOR)
            .headline(headline)
            .expireAfter(Duration.of(5, ChronoUnit.MINUTES))
            .detail(detail)
            .icon(new ItemStack(Items.BOWL))
            .build();
   }
}
