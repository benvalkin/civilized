package com.uncreated.civilized.core.villagerinfo;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import com.uncreated.civilized.core.InMemoryDB;
import com.uncreated.civilized.core.SetIndex;

import lombok.Getter;

public class VillagerInfoDB extends InMemoryDB<UUID, VillagerInfo> {

   @Getter
   private final SetIndex<UUID, VillagerInfo> settlementsToVillagersIndex = new SetIndex<>();

   private final Map<VillagerInfo, UUID> indexedSettlementIds = new IdentityHashMap<>();

   @Override
   protected UUID getKey(VillagerInfo obj) {
      return obj.getVillagerId();
   }

   @Override
   protected void index(VillagerInfo obj) {
      settlementsToVillagersIndex.add(obj.getSettlementId(), obj);
      indexedSettlementIds.put(obj, obj.getSettlementId());
   }

   @Override
   protected void unindex(VillagerInfo obj) {
      if (!indexedSettlementIds.containsKey(obj))
         return;

      settlementsToVillagersIndex.remove(indexedSettlementIds.remove(obj), obj);
   }
}
