package com.uncreated.civilized.core.recruitment;

import java.util.List;
import java.util.UUID;

import com.uncreated.civilized.core.building.BuildingStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerStore;

public record Impression(List<AttractivenessSource> attractiveness, int hospitality, int required) {

   public static Impression compute(
         VillagerInfo visitor,
         UUID settlementId,
         BuildingStore buildingStore,
         VillagerStore villagerStore) {
      return new Impression(
            SettlementAttractiveness.compute(settlementId, buildingStore, villagerStore),
            visitor.getHospitality().getWholePoints(),
            impressionRequired(visitor));
   }

   private static int impressionRequired(VillagerInfo visitor) {
      ImpressionRequirement impressionRequirement = visitor.getNpcRole().recruitTier();
      if (impressionRequirement == ImpressionRequirement.NOT_RECRUITABLE
            || impressionRequirement == ImpressionRequirement.LOW)
         // temperament multiplier doesn't work on not recruitable due to int overflow, and LOW is best left constant to
         // avoid accidentally making it impossible to acquire starter villagers
         return impressionRequirement.impressionRequired();

      float temperamentMultiplier = visitor.perLifetimeRandom(5).nextInt(80, 120) / 100f;
      // multiplied and rounded to nearest 5
      return (int) Math.round(impressionRequirement.impressionRequired() * temperamentMultiplier / 5.0) * 5;
   }

   public int total() {
      return SettlementAttractiveness.total(attractiveness) + hospitality;
   }

   public boolean isWillingToJoin() {
      return total() >= required;
   }
}
