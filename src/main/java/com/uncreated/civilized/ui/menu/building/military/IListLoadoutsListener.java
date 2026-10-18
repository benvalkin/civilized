package com.uncreated.civilized.ui.menu.building.military;

import java.util.List;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;

public interface IListLoadoutsListener {

   /**
    * @param requestId
    *           the id of the request this answers, which may be an old one the listener has since replaced
    */
   void receiveListLoadouts(int requestId, List<Loadout> results);
}
