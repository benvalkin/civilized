package com.uncreated.civilized.core.settlement.tax;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
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
import com.uncreated.civilized.core.villagerinfo.VillagerStore;
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
      long nextTaxCollectionDay = state.getNextTaxCollectionDay();
      // second check catches the clock being set back /time set which would otherwise leave us waiting until the old
      // date came around again (it may be very far in the future)
      if (nextTaxCollectionDay == SettlementState.NOT_SCHEDULED
            || nextTaxCollectionDay - day > TAX_COLLECTION_INTERVAL_DAYS) {
         scheduleNextCollection(state, day);
         return;
      }

      // not ready to collect.
      if (day < nextTaxCollectionDay)
         return;

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

      int taxCollected = 0;

      Set<Building> buildings =
            ServerBuildingsStore.INSTANCE.findForSettlement(loadedSettlement.getSettlement().getSettlementId());
      for (Building building : buildings) {

         if (!buildingPaysTax(building, ServerVillagerStore.INSTANCE))
            continue;

         int tax = getTaxAmount(building.getUpgradeLevel());
         taxCollected += tax;
      }

      if (taxCollected == 0)
         return;

      float townHallMultiplier = getTownHallTaxMultiplier(townHall.get());
      taxCollected = Math.round(taxCollected * townHallMultiplier);

      CurrencyItem.credit(loadedTownHall.get().chests(), taxCollected);
      NotificationService.INSTANCE.sendNotification(taxCollected(loadedSettlement.getSettlement(), taxCollected));
   }

   private static void scheduleNextCollection(SettlementState data, long today) {
      data.setNextTaxCollectionDay(today + TAX_COLLECTION_INTERVAL_DAYS);
      ServerSettlementsStore.INSTANCE.setDirty();
   }

   public static boolean buildingPaysTax(Building building, VillagerStore villagerStore) {
      // currently, only buildings where a spouse can live will pay tax
      if (!building.getBuildingType().isSpouseResidence())
         return false;

      List<VillagerInfo> residents =
            building.getOccupantIds().stream().map(villagerStore::find).flatMap(Optional::stream).toList();
      // any non-single villager living here makes the building pay tax
      return residents.stream().anyMatch(VillagerInfo::isTaken);
   }

   public static int getTaxAmount(Building building, ServerVillagerStore serverVillagerStore) {
      if (!buildingPaysTax(building, serverVillagerStore))
         return 0;

      return getTaxAmount(building.getUpgradeLevel());
   }

   public static int getTaxAmount(int buildingLevel) {

      return switch (buildingLevel) {
      case 1 -> 30;
      case 2 -> 50;
      case 3 -> 80;
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
