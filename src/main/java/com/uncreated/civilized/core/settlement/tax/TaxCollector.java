package com.uncreated.civilized.core.settlement.tax;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.core.notifications.Receiver;
import com.uncreated.civilized.core.notifications.Severity;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.SettlementState;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.neoforge.registration.ItemRegistry;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class TaxCollector {
   public static final int TAX_COLLECTION_INTERVAL_DAYS = 5;

   private final LoadedSettlement loadedSettlement;

   public TaxCollector(LoadedSettlement loadedSettlement) {
      this.loadedSettlement = loadedSettlement;
   }

   public void serverTick(ServerLevel level, long gameTime, long dayTime) {

      // (explaining this so that i don't forget again) dayTime keeps counting up across days, even when time is
      // advanced, that's why it works and gameTime doesn't
      long day = dayTime / 24000;
      SettlementState state = loadedSettlement.getSettlement().getState();

      long nextTaxAccrualDay = state.getNextTaxAccrualDay();
      // second check catches the clock being set back /time set which would otherwise leave us waiting until the old
      // date came around again (it may be very far in the future)
      if (nextTaxAccrualDay == SettlementState.NOT_SCHEDULED || nextTaxAccrualDay - day > 1) {
         scheduleNextAccrual(state, day);
         return;
      }

      long nextTaxCollectionDay = state.getNextTaxCollectionDay();
      if (nextTaxCollectionDay == SettlementState.NOT_SCHEDULED
            || nextTaxCollectionDay - day > TAX_COLLECTION_INTERVAL_DAYS) {
         scheduleNextCollection(state, day);
         return;
      }

      if (day >= nextTaxAccrualDay) {
         scheduleNextAccrual(state, day);

         Optional<Building> townHall =
               ServerBuildingsStore.INSTANCE.findTownHall(loadedSettlement.getSettlement().getSettlementId());
         if (townHall.isEmpty())
            return; // no town hall means the settlement does not accrue any taxes

         int newlyAccruedTax = 0;

         Set<Building> buildings =
               ServerBuildingsStore.INSTANCE.findForSettlement(loadedSettlement.getSettlement().getSettlementId());
         for (Building building : buildings) {
            int dailyAccrued = getDailyTaxForBuilding(building);
            newlyAccruedTax += dailyAccrued;
         }

         float townHallMultiplier = getTownHallTaxMultiplier(townHall.get());
         // accruedDays is usually 1, but also account for missed days when the settlement wasn't loaded
         long accruedDays = Math.min(day - nextTaxAccrualDay + 1, TAX_COLLECTION_INTERVAL_DAYS);
         newlyAccruedTax = Math.round(newlyAccruedTax * accruedDays * townHallMultiplier);
         state.setTotalAccruedTax(state.getTotalAccruedTax() + newlyAccruedTax);
         ServerSettlementsStore.INSTANCE.setDirty();
         ServerSettlementsStore.INSTANCE.replicateChange(loadedSettlement.getSettlement(), StoreOperation.UPDATE);
      }

      if (day >= nextTaxCollectionDay) {
         long timeOfDay = dayTime % 24000;
         boolean onTheHour = dayTime % 1000 == 0;
         // avoid trying to collect taxes every tick or at night
         boolean collectionTime = timeOfDay >= 6000 && timeOfDay <= 12000 && onTheHour;
         if (!collectionTime)
            return;

         Optional<Building> townHall =
               ServerBuildingsStore.INSTANCE.findTownHall(loadedSettlement.getSettlement().getSettlementId());
         if (townHall.isEmpty())
            return; // must have a town hall to collect taxes

         // TODO: should eventually force the townhall to load
         Optional<LoadedBuilding> loadedTownHall = LoadedBuildings.checkLoaded(townHall.get());
         if (loadedTownHall.isEmpty())
            return; // tried again next tick, so the collection happens as soon as the town hall is loaded

         scheduleNextCollection(state, day);

         int taxCollected = state.getTotalAccruedTax();
         if (taxCollected == 0)
            return;

         CurrencyItem.credit(loadedTownHall.get().chests(), taxCollected);

         state.setTotalAccruedTax(0);
         ServerSettlementsStore.INSTANCE.setDirty();
         ServerSettlementsStore.INSTANCE.replicateChange(loadedSettlement.getSettlement(), StoreOperation.UPDATE);

         NotificationService.INSTANCE.sendNotification(taxCollected(loadedSettlement.getSettlement(), taxCollected));
      }
   }

   private static void scheduleNextAccrual(SettlementState data, long today) {
      data.setNextTaxAccrualDay(today + 1);
      ServerSettlementsStore.INSTANCE.setDirty();
   }

   private static void scheduleNextCollection(SettlementState data, long today) {
      data.setNextTaxCollectionDay(today + TAX_COLLECTION_INTERVAL_DAYS);
      ServerSettlementsStore.INSTANCE.setDirty();
   }

   public static int getDailyTaxForBuilding(Building building) {
      // currently, only family residences will pay tax
      if (!building.getBuildingType().isFamilyResidence())
         return 0;

      float dailyTax = 0;
      List<VillagerInfo> residents = BuildingUtil.getOccupants(building, ServerVillagerStore.INSTANCE);
      for (VillagerInfo resident : residents) {
         if (resident.isChild())
            continue;

         if (resident.getOccupation().is(VillagerOccupations.SOLDIER))
            continue;

         Optional<VillagerInfo> partner = ServerVillagerStore.INSTANCE.find(resident.getPartnerId());
         if (partner.isEmpty())
            continue;

         boolean livesWithPartner = residents.contains(partner.get());
         if (!livesWithPartner)
            continue;
         // todo: should we really make villagers only pay tax if they live together? separated parents will be tax
         // exempt

         dailyTax += getDailyTaxForBuildingLevel(building.getUpgradeLevel()) / 2f;
      }

      return Math.round(dailyTax);
   }

   public static int getDailyTaxForBuildingLevel(int buildingLevel) {

      return switch (buildingLevel) {
      case 1 -> 4;
      case 2 -> 8;
      case 3 -> 12;
      default -> 0;
      };
   }

   public static float getTownHallTaxMultiplier(Building townHall) {
      // potentially add a town hall option to control tax rate later
      return 1;
   }

   public static Notification taxCollected(Settlement settlement, int amount) {

      Component headline = Component.translatable("notification.tax.taxes_collected");

      Component detail =
            Component.translatable(
                  "notification.tax.taxes_collected.detail",
                  settlement.getDisplayName(),
                  CurrencyItem.amountTranslation(amount).withColor(Colors.COIN));

      return Notification.builder()
            .type("taxes_collected")
            .settlementId(settlement.getSettlementId())
            .receiver(Receiver.ADMINISTRATION)
            .severity(Severity.POSITIVE)
            .headline(headline)
            .expireAfter(Duration.of(10, ChronoUnit.MINUTES))
            .detail(detail)
            .icon(new ItemStack(ItemRegistry.COIN_STACK.get()))
            .build();
   }
}
