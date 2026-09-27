package com.uncreated.civilized.core.settlement.permission.events;

import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;

import lombok.Getter;
import net.neoforged.bus.api.Event;

@Getter
public class SettlementPermissionsUpdatedEvent extends Event {

   private final SettlementPermissions permissions;
   private final boolean isClientside;

   public SettlementPermissionsUpdatedEvent(SettlementPermissions permissions, boolean isClientside) {
      this.permissions = permissions;
      this.isClientside = isClientside;
   }
}
