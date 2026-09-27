package com.uncreated.civilized.core.settlement.permission;

public enum AccessLevel {
   FRIEND(1), // can open building doors and pickup items
   CITIZEN(2), // can place/destroy blocks and open only food vendor chests
   ADMINISTRATOR(3), // can open chests and edit certain parts buildings (e.g. production recipes)
   GOVERNOR(4); // can edit/create/upgrade buildings

   private final int level;

   AccessLevel(int level) {
      this.level = level;
   }

   public boolean isAboveOrEqualTo(AccessLevel inclusive) {
      return this.level >= inclusive.level;
   }

   public boolean isBelow(AccessLevel exclusive) {
      return this.level < exclusive.level;
   }
}
