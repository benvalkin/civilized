package com.uncreated.civilized.core.building.logistics.hauling;

import com.uncreated.civilized.entity.CivilizedVillager;

public record ReservationKey(String party, String name) {

   @Override
   public String toString() {
      return name + " (" + party + ")";
   }

   public static String partyKeyFor(CivilizedVillager villager, String activityName) {
      return villager.getInfo().getVillagerId().toString() + ":" + activityName;
   }
}
