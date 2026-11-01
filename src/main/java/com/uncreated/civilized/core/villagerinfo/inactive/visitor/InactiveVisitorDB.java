package com.uncreated.civilized.core.villagerinfo.inactive.visitor;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import com.uncreated.civilized.core.SetIndex;
import com.uncreated.civilized.core.villagerinfo.inactive.InactiveVillagerDB;

import lombok.Getter;

public class InactiveVisitorDB extends InactiveVillagerDB<RecurringVisitorRecord> {

   @Getter
   private final SetIndex<UUID, RecurringVisitorRecord> settlementsToVillagersIndex = new SetIndex<>();
   private final Map<RecurringVisitorRecord, UUID> indexedSettlementIds = new IdentityHashMap<>();

   @Override
   protected void index(RecurringVisitorRecord obj) {
      settlementsToVillagersIndex.add(obj.getInfo().getVisitingSettlementId(), obj);
      indexedSettlementIds.put(obj, obj.getInfo().getVisitingSettlementId());
   }

   @Override
   protected void unindex(RecurringVisitorRecord obj) {
      UUID settlementId = indexedSettlementIds.remove(obj);
      if (settlementId != null)
         settlementsToVillagersIndex.remove(settlementId, obj);
   }
}
