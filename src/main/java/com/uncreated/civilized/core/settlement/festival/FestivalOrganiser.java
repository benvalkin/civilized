package com.uncreated.civilized.core.settlement.festival;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.entity.behaviour.TownSquareBehaviour;
import com.uncreated.civilized.core.building.util.BuildingUtil;
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
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.util.random.DailyEventScheduler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Runs a settlement's festivals once they've been started: brings in suitors, and wraps the festival up at the end. */
public class FestivalOrganiser {

   private static final float SUITORS_PER_SINGLE_WORKER = 1.25F;
   private static final int MINIMUM_FESTIVAL_VISITORS = 6;
   private final LoadedSettlement loadedSettlement;
   private @Nullable DailyEventScheduler visitorArrivalScheduler;
   private long visitorArrivalsFestivalStartTime = SettlementState.NOT_SCHEDULED;
   /** The phase seen last tick, to notice when the celebration begins. Null until the first tick. */
   private @Nullable Festival.Phase lastPhase;

   public FestivalOrganiser(LoadedSettlement loadedSettlement) {
      this.loadedSettlement = loadedSettlement;
   }

   private Settlement settlement() {
      return loadedSettlement.getSettlement();
   }

   /** Starts a festival, assuming that it's been checked to be allowed, and that it's been paid for. */
   public static void start(Settlement settlement, long dayTime) {
      SettlementState state = settlement.getState();
      state.setFestivalStartTime(dayTime);
      state.setLastFestivalDay(dayTime / 24000);
      state.setFestivalsHeldCount(state.getFestivalsHeldCount() + 1);
      ServerSettlementsStore.INSTANCE.setDirty();
      ServerSettlementsStore.INSTANCE.replicateChange(settlement, StoreOperation.UPDATE);

      NotificationService.INSTANCE.sendNotification(
            festivalNotification(
                  settlement,
                  "festival_announced",
                  "notification.festival.announced",
                  "notification.festival.announced.detail"));
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

      if (phase != Festival.Phase.GATHERING)
         visitorArrivalScheduler = null;

      switch (phase) {
      case NONE -> end(level, dayTime);
      case GATHERING -> {
         if (visitorArrivalScheduler == null || visitorArrivalsFestivalStartTime != state.getFestivalStartTime())
            visitorArrivalScheduler = scheduleSuitorArrivals(state);
         visitorArrivalScheduler.tick(level, gameTime, () -> trySpawnVisitor(level, state));
      }
      case CELEBRATING -> {
         if (celebrationBegan)
            NotificationService.INSTANCE.sendNotification(
                  festivalNotification(
                        settlement(),
                        "festival_started",
                        "notification.festival.started",
                        "notification.festival.started.detail"));
      }
      }
   }

   private Optional<Building> findTownSquare() {
      return ServerBuildingsStore.INSTANCE.findTownSquare(settlement().getSettlementId());
   }

   private DailyEventScheduler scheduleSuitorArrivals(SettlementState state) {
      int numberOfSingleWorkers =
            (int) ServerVillagerStore.INSTANCE.getCitizens(settlement().getSettlementId())
                  .stream()
                  .filter(villager -> villager.getNpcRole().is(VillagerNpcRoles.WORKER) && !villager.isTaken())
                  .count();
      int numberOfVisitors =
            Math.max(Mth.ceil(numberOfSingleWorkers * SUITORS_PER_SINGLE_WORKER), MINIMUM_FESTIVAL_VISITORS);

      int gatheringStartTime = (int) (state.getFestivalStartTime() % 24000);
      int gatheringEndTime = gatheringStartTime + Festival.GATHERING_DURATION;
      visitorArrivalsFestivalStartTime = state.getFestivalStartTime();
      return new DailyEventScheduler(numberOfVisitors, gatheringStartTime, gatheringEndTime, 1F);
   }

   private void trySpawnVisitor(ServerLevel level, SettlementState state) {
      Optional<Building> townSquare = findTownSquare();
      // a spot to spawn at can only be found in loaded chunks
      if (townSquare.isEmpty() || LoadedBuildings.checkLoaded(townSquare.get()).isEmpty())
         return;

      TownSquareBehaviour.spawnVisitor(level, townSquare.get(), VillagerNpcRoles.SUITOR, null)
            // suitors stay until the festival is over instead of their normal departure time (they will still finish
            // whatever conversation they're having)
            .flatMap(VisitorBehaviour::of)
            .ifPresent(visitor -> visitor.setDepartAt(Festival.endTime(state)));
   }

   private void end(ServerLevel level, long dayTime) {
      settlement().getState().setFestivalStartTime(SettlementState.NOT_SCHEDULED);
      ServerSettlementsStore.INSTANCE.setDirty();
      ServerSettlementsStore.INSTANCE.replicateChange(settlement(), StoreOperation.UPDATE);

      // probably don't need to do this, but force all town square visitors to leave after the festival is done
      findTownSquare().ifPresent(
            townSquare -> BuildingUtil.getVisitors(townSquare, ServerVillagerStore.INSTANCE)
                  .stream()
                  .map(visitor -> LoadedVillagers.find(visitor.getVillagerId()))
                  .flatMap(Optional::stream)
                  .map(VisitorBehaviour::of)
                  .flatMap(Optional::stream)
                  .forEach(visitor -> visitor.setDepartAt(dayTime)));

      NotificationService.INSTANCE.sendNotification(
            festivalNotification(
                  settlement(),
                  "festival_ended",
                  "notification.festival.ended",
                  "notification.festival.ended.detail"));
   }

   private static Notification festivalNotification(
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
