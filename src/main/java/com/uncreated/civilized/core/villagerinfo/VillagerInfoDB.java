package com.uncreated.civilized.core.villagerinfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.uncreated.civilized.core.InMemoryDB;
import com.uncreated.civilized.core.SetIndex;

import lombok.Getter;

public class VillagerInfoDB extends InMemoryDB<UUID, VillagerInfo> {

   @Getter
   private final SetIndex<UUID, VillagerInfo> settlementsToVillagersIndex = new SetIndex<>();

   private final Map<VillagerInfo, UUID> indexedSettlementIds = new IdentityHashMap<>();

   @Getter
   private final SetIndex<UUID, VillagerInfo> parentsToChildrenIndex = new SetIndex<>();

   private final Map<VillagerInfo, List<UUID>> indexedParentIds = new IdentityHashMap<>();

   @Override
   protected UUID getKey(VillagerInfo obj) {
      return obj.getVillagerId();
   }

   @Override
   protected void index(VillagerInfo obj) {
      settlementsToVillagersIndex.add(obj.getSettlementId(), obj);
      indexedSettlementIds.put(obj, obj.getSettlementId());

      List<UUID> parentIds = obj.getParentIds();
      for (UUID parentId : parentIds)
         parentsToChildrenIndex.add(parentId, obj);
      indexedParentIds.put(obj, parentIds);
   }

   @Override
   protected void unindex(VillagerInfo obj) {
      List<UUID> parentIds = indexedParentIds.remove(obj);
      if (parentIds != null) {
         for (UUID parentId : parentIds)
            parentsToChildrenIndex.remove(parentId, obj);
      }

      if (!indexedSettlementIds.containsKey(obj))
         return;

      settlementsToVillagersIndex.remove(indexedSettlementIds.remove(obj), obj);
   }
}
