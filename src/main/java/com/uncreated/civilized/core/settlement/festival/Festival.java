package com.uncreated.civilized.core.settlement.festival;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.settlement.SettlementState;
import com.uncreated.civilized.core.settlement.SettlementsStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.chat.Component;

public final class Festival {

   public static final int BASE_COST = 100;
   /** How much more each festival costs after the one before it. */
   public static final int COST_INCREASE = 70;
   public static final int COOLDOWN_DAYS = 10;
   public static final int EARLIEST_START_TIME = 8000; // 2pm
   public static final int LATEST_START_TIME = 14000; // 8pm
   /** How long visitors spend arriving before the celebration begins. */
   public static final int GATHERING_DURATION = 2000; // 2 hours
   public static final int CELEBRATION_DURATION = 4000; // 4 hours
   public static final int FLIRTING_SUCCESS_MULTIPLIER = 2;

   public enum Phase {
      NONE,
      /** Visitors are arriving in the town square. */
      GATHERING,
      /** Festival is going on. */
      CELEBRATING
   }

   private Festival() {
   }

   public static int cost(SettlementState state) {
      return BASE_COST + COST_INCREASE * state.getFestivalsHeldCount();
   }

   public static boolean isUnderway(SettlementState state) {
      return state.getFestivalStartTime() != SettlementState.NOT_SCHEDULED;
   }

   public static Phase phase(SettlementState state, long dayTime) {
      long startTime = state.getFestivalStartTime();
      if (startTime == SettlementState.NOT_SCHEDULED || dayTime < startTime)
         return Phase.NONE;

      long elapsed = dayTime - startTime;
      if (elapsed < GATHERING_DURATION)
         return Phase.GATHERING;
      if (elapsed < GATHERING_DURATION + CELEBRATION_DURATION)
         return Phase.CELEBRATING;
      return Phase.NONE;
   }

   public static long endTime(SettlementState state) {
      return state.getFestivalStartTime() + GATHERING_DURATION + CELEBRATION_DURATION;
   }

   public static Optional<Component> whyCannotStart(SettlementState state, long dayTime, int availableCoins) {
      if (isUnderway(state))
         return Optional.of(Component.translatable("festival.cannot_start.underway"));

      long day = dayTime / 24000;
      long nextAvailableDay = state.getLastFestivalDay() + COOLDOWN_DAYS;
      if (state.getLastFestivalDay() != SettlementState.NOT_SCHEDULED && day < nextAvailableDay
            && state.getLastFestivalDay() <= day)
         return Optional.of(Component.translatable("festival.cannot_start.cooldown", nextAvailableDay - day));

      long timeOfDay = dayTime % 24000;
      if (timeOfDay < EARLIEST_START_TIME || timeOfDay > LATEST_START_TIME)
         return Optional.of(Component.translatable("festival.cannot_start.wrong_time"));

      if (availableCoins < cost(state))
         return Optional.of(Component.translatable("festival.cannot_start.not_enough_coins", costTranslation(state)));

      return Optional.empty();
   }

   public static Component costTranslation(SettlementState state) {
      return CurrencyItem.amountTranslation(cost(state)).withColor(Colors.COIN);
   }

   public static @Nullable UUID findHostSettlementId(VillagerInfo villager, BuildingStore buildings) {
      if (villager.getSettlementId() != null)
         return villager.getSettlementId();

      return buildings.find(villager.getHomeBuildingId()).map(Building::getSettlementId).orElse(null);
   }

   public static Phase currentPhaseFor(
         VillagerInfo villager,
         BuildingStore buildings,
         SettlementsStore settlements,
         long dayTime) {
      UUID settlementId = findHostSettlementId(villager, buildings);
      if (settlementId == null)
         return Phase.NONE;

      return settlements.find(settlementId).map(s -> phase(s.getState(), dayTime)).orElse(Phase.NONE);
   }
}
