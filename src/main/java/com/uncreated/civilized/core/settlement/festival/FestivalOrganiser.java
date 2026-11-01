package com.uncreated.civilized.core.settlement.festival;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.core.notifications.Receiver;
import com.uncreated.civilized.core.notifications.Severity;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.SettlementState;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.VisitorBehaviour;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Runs a settlement's festivals once they've been started: entertains visitors, and wraps the festival up at the end.
 */
public class FestivalOrganiser {

   private final LoadedSettlement loadedSettlement;
   /** The phase seen last tick, to notice when the celebration begins. Null until the first tick. */
   private @Nullable Festival.Phase lastPhase;

   public FestivalOrganiser(LoadedSettlement loadedSettlement) {
      this.loadedSettlement = loadedSettlement;
   }

   private Settlement settlement() {
      return loadedSettlement.getSettlement();
   }

   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      SettlementState state = settlement().getState();
      if (!Festival.isUnderway(state)) {
         lastPhase = Festival.Phase.NONE;
         return;
      }

      Festival.Phase phase = Festival.phase(state, dayTime);
      // the server may have restarted mid-celebration, which shouldn't announce it again
      boolean celebrationBegan = lastPhase == Festival.Phase.GATHERING && phase == Festival.Phase.CELEBRATING;
      lastPhase = phase;

      if (phase == Festival.Phase.NONE) {
         end();
         return;
      }

      if (celebrationBegan) {
         entertainVisitors();
         NotificationService.INSTANCE.sendNotification(
               festivalNotification(
                     settlement(),
                     "festival_started",
                     "notification.festival.started",
                     "notification.festival.started.detail"));
      }
   }

   private void entertainVisitors() {
      for (VillagerInfo visitor : ServerVillagerStore.INSTANCE.getVisitors(settlement().getSettlementId())) {
         LoadedVillagers.find(visitor.getVillagerId())
               .flatMap(VisitorBehaviour::tryGet)
               .ifPresent(VisitorBehaviour::gainHospitalityFromFestival);
      }
   }

   private void end() {
      settlement().getState().setFestivalStartTime(SettlementState.NOT_SCHEDULED);
      ServerSettlementsStore.INSTANCE.setDirty();
      ServerSettlementsStore.INSTANCE.replicateChange(settlement(), StoreOperation.UPDATE);

      NotificationService.INSTANCE.sendNotification(
            festivalNotification(
                  settlement(),
                  "festival_ended",
                  "notification.festival.ended",
                  "notification.festival.ended.detail"));
   }

   public static Notification festivalNotification(
         Settlement settlement,
         String type,
         String headlineKey,
         String detailKey) {
      return Notification.builder()
            .type(type)
            .settlementId(settlement.getSettlementId())
            .receiver(Receiver.ADMINISTRATION)
            .severity(Severity.POSITIVE)
            .headline(Component.translatable(headlineKey))
            .detail(Component.translatable(detailKey, settlement.getDisplayName()))
            .expireAfter(Duration.of(5, ChronoUnit.MINUTES))
            .icon(new ItemStack(Items.CAKE))
            .build();
   }
}
