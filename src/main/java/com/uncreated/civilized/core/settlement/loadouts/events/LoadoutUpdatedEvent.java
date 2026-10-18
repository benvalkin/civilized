package com.uncreated.civilized.core.settlement.loadouts.events;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;

import lombok.Getter;
import net.neoforged.bus.api.Event;

@Getter
public class LoadoutUpdatedEvent extends Event {

   private final Loadout loadout;
   private final boolean isClientside;

   public LoadoutUpdatedEvent(Loadout loadout, boolean isClientside) {
      this.loadout = loadout;
      this.isClientside = isClientside;
   }
}
