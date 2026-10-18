package com.uncreated.civilized.ui.menu.building.military;

import java.util.List;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.networking.packets.LoadoutSavedResult;

public interface ILoadoutListener {

   /**
    * @param requestId
    *           the id of the request this answers, which may be an old one the listener has since replaced
    */
   default void receiveListLoadouts(int requestId, List<Loadout> results) {

   }

   default void receiveLoadoutSavedResult(LoadoutSavedResult result) {

   }
}
