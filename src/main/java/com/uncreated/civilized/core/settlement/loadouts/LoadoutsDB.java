package com.uncreated.civilized.core.settlement.loadouts;

import java.util.UUID;

import com.uncreated.civilized.core.InMemoryDB;

public class LoadoutsDB extends InMemoryDB<UUID, Loadout> {

   @Override
   protected UUID getKey(Loadout obj) {
      return obj.loadoutId();
   }

}
