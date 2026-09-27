package com.uncreated.civilized.core.settlement.permission;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public enum AccessLevel {
   // FRIEND(1), // can open building doors and pickup items
   CITIZEN(1), // can place/destroy blocks and open only food vendor chests
   ADMINISTRATOR(2), // can open chests and edit certain parts buildings (e.g. production recipes)
   GOVERNOR(3); // can edit/create/upgrade buildings

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

   public Component translation() {
      return Component.translatable("settlement.access_level." + name().toLowerCase(Locale.ROOT));
   }

   /** What a player with this access level may do. */
   public Component description() {
      return Component.translatable("settlement.access_level." + name().toLowerCase(Locale.ROOT) + ".description");
   }

   public static Component noAccessTranslation() {
      return Component.translatable("settlement.access_level.none");
   }

   public static Component noAccessDescription() {
      return Component.translatable("settlement.access_level.none.description");
   }
}
