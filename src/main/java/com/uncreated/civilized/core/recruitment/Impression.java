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
            visitor.getNpcRole().recruitTier().impressionRequired());
   }

   public int total() {
      return SettlementAttractiveness.total(attractiveness) + hospitality;
   }

   public boolean isWillingToJoin() {
      return total() >= required;
   }
}
