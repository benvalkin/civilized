package com.uncreated.civilized.core.notifications;

import java.util.List;

import com.uncreated.civilized.core.settlement.permission.AccessLevel;

public enum Receiver {
   OWNER, ADMINISTRATION, CITIZENS, VISITORS;

   public static List<Receiver> fromAccessLevel(AccessLevel accessLevel) {
      return switch (accessLevel) {
      case GOVERNOR -> List.of(OWNER, ADMINISTRATION, CITIZENS);
      case ADMINISTRATOR -> List.of(ADMINISTRATION, CITIZENS);
      case CITIZEN -> List.of(CITIZENS);
      };
   }
}
