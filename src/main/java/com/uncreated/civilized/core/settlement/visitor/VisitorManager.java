package com.uncreated.civilized.core.settlement.visitor;

import static net.minecraft.network.chat.Component.translatable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.core.notifications.Receiver;
import com.uncreated.civilized.core.notifications.Severity;
import com.uncreated.civilized.core.recruitment.ImpressionRequirement;
import com.uncreated.civilized.core.recruitment.SettlementAttractiveness;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.trading.MerchantType;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRole;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.core.villagerinfo.inactive.visitor.RecurringVisitorRecord;
import com.uncreated.civilized.core.villagerinfo.inactive.visitor.ServerRecurringVisitorStore;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.MerchantVisitorBehaviour;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;
import com.uncreated.civilized.util.random.DailyEventScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class VisitorManager {

   private static final Logger LOGGER = LogUtils.getLogger();

   public static final int MAX_DAY_VISITORS = 8;
   private static final int SPAWN_SPOT_ATTEMPTS = 16;

   // visitors may leave at any time from 8pm, and are guaranteed to be gone by 10pm
   private static final int EARLIEST_DEPARTURE_TIME = 14000;
   private static final int LATEST_DEPARTURE_TIME = 16000;

   public static final float RETURN_CHANCE = 0.3f;

   /** Every role that can visit a settlement */
   private static final List<VillagerNpcRole> VISITOR_ROLES =
         List.of(
               VillagerNpcRoles.TRAVELLER,
               VillagerNpcRoles.MIGRANT,
               VillagerNpcRoles.SUITOR,
               VillagerNpcRoles.SKILLED_PROFESSIONAL,
               VillagerNpcRoles.MERCENARY,
               VillagerNpcRoles.BEGGAR,
               VillagerNpcRoles.SCOUNDREL,
               VillagerNpcRoles.THIEF,
               VillagerNpcRoles.MERCHANT);

   private final LoadedSettlement loadedSettlement;
   private final DailyEventScheduler eventScheduler;

   public VisitorManager(LoadedSettlement loadedSettlement) {
      this.loadedSettlement = loadedSettlement;
      eventScheduler = new DailyEventScheduler(8, 0, 6000, 0.2f);
   }

   public static List<VillagerNpcRole> visitorRoles() {
      return VISITOR_ROLES;
   }

   private UUID settlementId() {
      return loadedSettlement.getSettlement().getSettlementId();
   }

   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      eventScheduler.tick(level, gameTime, () -> trySpawnRandomVisitor(level));
   }

   private void trySpawnRandomVisitor(ServerLevel level) {
      if (countDayVisitors() >= MAX_DAY_VISITORS)
         return;

      if (level.getRandom().nextFloat() < RETURN_CHANCE && trySpawnReturningVisitor(level))
         return;

      int attractiveness =
            SettlementAttractiveness.total(
                  SettlementAttractiveness
                        .compute(settlementId(), ServerBuildingsStore.INSTANCE, ServerVillagerStore.INSTANCE));

      visitorWeights(attractiveness).getRandomValue(level.getRandom())
            .ifPresent(role -> spawnVisitor(level, role, null));
   }

   private int countDayVisitors() {
      int count = 0;
      for (VillagerInfo visitor : ServerVillagerStore.INSTANCE.getVisitors(settlementId())) {
         if (visitor.getHomeBuildingId() == null)
            count++;
      }
      return count;
   }

   private static SimpleWeightedRandomList<VillagerNpcRole> visitorWeights(int attractiveness) {
      int low;
      int medium;
      if (attractiveness < 15) {
         low = 3;
         medium = 0;
      } else if (attractiveness < 20) {
         low = 1;
         medium = 2;
      } else {
         low = 0;
         medium = 3;
      }

      SimpleWeightedRandomList.Builder<VillagerNpcRole> weights = new SimpleWeightedRandomList.Builder<>();
      weights.add(VillagerNpcRoles.BEGGAR, low);
      weights.add(VillagerNpcRoles.MIGRANT, medium);
      weights.add(VillagerNpcRoles.MERCHANT, 1);
      return weights.build();
   }

   /**
    * Spawns a day visitor at the town square, or otherwise the town hall. Day visitors will depart at the end of the
    * day, or try to move into an empty Inn if there is one.
    *
    * @param merchantType
    *           if spawning a merchant, specifies the merchant type. If null, a random merchant type is picked.
    */
   public Optional<CivilizedVillager> spawnVisitor(
         ServerLevel level,
         VillagerNpcRole role,
         @Nullable Holder.Reference<MerchantType> merchantType) {

      Optional<BlockPos> spawnPos = findArrivalPos(level, role);
      if (spawnPos.isEmpty())
         return Optional.empty();

      CivilizedVillager villager =
            EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, spawnPos.get(), EntitySpawnReason.EVENT);

      if (villager == null) {
         LOGGER.error("Tried to spawn a visitor, but something went wrong during the entity spawning process.");
         return Optional.empty();
      }

      villager.changeNpcRole(role);
      if (villager.getRoleBehaviour() instanceof MerchantVisitorBehaviour merchant) {
         if (merchantType != null)
            merchant.stockWithNewTrades(merchantType);
         else
            merchant.stockWithNewTrades(level);
      }
      villager.getInfo().setVisitingSettlementId(settlementId());
      VisitorBehaviour.tryGet(villager).ifPresent(visitor -> {
         visitor.setDepartAt(chooseDepartureTime(level.getDayTime(), villager.getRandom()));
         visitor.resetPerVisitStats(level.getGameTime());
      });
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.UPDATE);

      // villager's activities may have changed with its role so we need refresh its brain
      villager.refreshBrain(level);

      LOGGER.debug("Visitor spawned in settlement {}: {}", settlementId(), villager.getUUID());
      return Optional.of(villager);
   }

   /**
    * Spawns a random visitor who has previously visited the settlement before, if one exists.
    *
    * @return true if the visitor was successfully spawned, false if there were no returning visitors to pick from, or
    *         they couldn't spawn in properly.
    */
   private boolean trySpawnReturningVisitor(ServerLevel level) {
      long today = level.getDayTime() / 24000;
      Optional<RecurringVisitorRecord> returning =
            ServerRecurringVisitorStore.INSTANCE.findRandomReturningVisitor(settlementId(), today, level.getRandom());
      if (returning.isEmpty())
         return false;

      VillagerInfo info = returning.get().getInfo();
      Optional<BlockPos> spawnPos = findArrivalPos(level, info.getNpcRole());
      if (spawnPos.isEmpty())
         return false;

      // the info has to be back in the store before the entity joins the level, otherwise the entity will throw
      // exceptions
      ServerRecurringVisitorStore.INSTANCE.remove(info.getVillagerId());
      ServerVillagerStore.INSTANCE.restoreVillager(info);

      CivilizedVillager villager =
            EntityRegistry.CIVILIZED_VILLAGER.get()
                  .spawn(
                        level,
                        v -> v.continueVillager(info.getVillagerId()),
                        spawnPos.get(),
                        EntitySpawnReason.EVENT,
                        false,
                        false);

      if (villager == null) {
         LOGGER.error(
               "Tried to spawn a returning visitor, but something went wrong during the entity spawning process.");
         ServerVillagerStore.INSTANCE.removeVillager(info, false);
         ServerRecurringVisitorStore.INSTANCE.add(returning.get());
         return false;
      }

      VisitorBehaviour.tryGet(villager).ifPresent(visitor -> {
         visitor.setDepartAt(chooseDepartureTime(level.getDayTime(), villager.getRandom()));
         visitor.restoreFromPreviousVisit(returning.get().getVisits(), returning.get().getAccruedHospitality());
         visitor.resetPerVisitStats(level.getGameTime());
         sendReturnedNotification(info, visitor.getNumberOfVisits());
      });
      villager.refreshBrain(level);

      LOGGER.debug("Visitor returned to settlement {}: {}", settlementId(), villager.getUUID());
      return true;
   }

   private Optional<BlockPos> findArrivalPos(ServerLevel level, VillagerNpcRole role) {
      Optional<Building> arrivalPlace = findArrivalBuilding(role);
      if (arrivalPlace.isEmpty() || LoadedBuildings.checkLoaded(arrivalPlace.get()).isEmpty()) {
         LOGGER.debug("Nowhere for visitor to arrive at in settlement (settlementId: {})", settlementId());
         return Optional.empty();
      }

      Optional<BlockPos> spawnPos =
            arrivalPlace.get().getBounds().findRandomStandableSpot(level, level.getRandom(), SPAWN_SPOT_ATTEMPTS);
      if (spawnPos.isEmpty())
         LOGGER.debug("Couldn't find valid spawn block pos for visitor at {}", arrivalPlace.get());

      return spawnPos;
   }

   private Optional<Building> findArrivalBuilding(VillagerNpcRole role) {
      Optional<Building> townSquare = ServerBuildingsStore.INSTANCE.findTownSquare(settlementId());
      if (townSquare.isPresent())
         return townSquare;

      boolean canArriveAtTownHall = role.recruitTier() == ImpressionRequirement.LOW;
      if (!canArriveAtTownHall)
         return Optional.empty();

      return ServerBuildingsStore.INSTANCE.findTownHall(settlementId());
   }

   private long chooseDepartureTime(long dayTime, RandomSource random) {
      long startOfDay = dayTime - dayTime % 24000;
      return startOfDay + EARLIEST_DEPARTURE_TIME + random.nextInt(LATEST_DEPARTURE_TIME - EARLIEST_DEPARTURE_TIME);
   }

   public static void sendReturnedNotification(VillagerInfo visitor, int visits) {
      ServerSettlementsStore.INSTANCE.find(visitor.getVisitingSettlementId())
            .ifPresent(
                  settlement -> NotificationService.INSTANCE.sendNotification(create(visitor, settlement, visits)));
   }

   private static Notification create(VillagerInfo visitor, Settlement settlement, int visits) {
      return Notification.builder()
            .type("visitor_returned")
            .settlementId(settlement.getSettlementId())
            .villagerId(visitor.getVillagerId())
            .receiver(Receiver.ADMINISTRATION)
            .severity(Severity.POSITIVE)
            .headline(translatable("notification.visitor.returned"))
            .detail(
                  translatable(
                        "notification.visitor.returned.detail",
                        visitor.getFirstName(),
                        visitor.getNpcRole().translation(visitor.getGender()),
                        settlement.displayNameTranslation(),
                        visits))
            .expireAfter(Duration.ofMinutes(20))
            .icon(new ItemStack(Items.BELL))
            .build();
   }
}
