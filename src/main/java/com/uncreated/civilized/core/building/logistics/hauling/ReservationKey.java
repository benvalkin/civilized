package com.uncreated.civilized.core.building.logistics.hauling;

public record ReservationKey(String party, String name) {

   @Override
   public String toString() {
      return name + " (" + party + ")";
   }
}
