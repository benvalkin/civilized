package com.uncreated.civilized.core.villagerinfo.inactive;

import java.util.UUID;

import com.uncreated.civilized.core.InMemoryDB;

public class InactiveVillagerDB<V extends IInactiveVillagerRecord> extends InMemoryDB<UUID, V> {

   @Override
   protected UUID getKey(V obj) {
      return obj.getInfo().getVillagerId();
   }
}
