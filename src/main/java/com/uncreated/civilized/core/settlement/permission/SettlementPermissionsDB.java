package com.uncreated.civilized.core.settlement.permission;

import java.util.UUID;

import com.uncreated.civilized.core.InMemoryDB;

public class SettlementPermissionsDB extends InMemoryDB<UUID, SettlementPermissions> {

   @Override
   protected UUID getKey(SettlementPermissions obj) {
      return obj.settlementId();
   }

}
