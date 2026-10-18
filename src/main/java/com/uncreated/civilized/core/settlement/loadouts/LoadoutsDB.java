package com.uncreated.civilized.core.settlement.loadouts;

import java.util.UUID;

import com.uncreated.civilized.core.InMemoryDB;
import com.uncreated.civilized.core.SetIndex;

import lombok.Getter;

public class LoadoutsDB extends InMemoryDB<UUID, Loadout> {

   @Getter
   private final SetIndex<UUID, Loadout> settlementsToLoadoutsIndex = new SetIndex<>();

   @Override
   protected UUID getKey(Loadout obj) {
      return obj.loadoutId();
   }

   @Override
   protected void index(Loadout obj) {
      settlementsToLoadoutsIndex.add(obj.settlementId(), obj);
   }

   @Override
   protected void unindex(Loadout obj) {
      settlementsToLoadoutsIndex.remove(obj.settlementId(), obj);
   }
}
