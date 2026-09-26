package com.uncreated.civilized.core.villagerinfo;

public enum Gender {
   MALE, FEMALE;

   public Gender opposite() {
      return switch (this) {
         case MALE ->  Gender.FEMALE;
         case FEMALE -> Gender.MALE;
      };
   }
}
