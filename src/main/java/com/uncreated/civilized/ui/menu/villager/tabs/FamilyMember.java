package com.uncreated.civilized.ui.menu.villager.tabs;

import com.uncreated.civilized.core.villagerinfo.Gender;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;

import net.minecraft.network.chat.Component;

public record FamilyMember(Relation relation, VillagerInfo info) {

   public enum Relation {
      PARTNER,
      PARENT,
      CHILD,
      SIBLING;

      public Component translation(Gender gender) {
         return Component.translatable(
               "menu.villager.family.relation." + name().toLowerCase() + "." + gender.toString().toLowerCase());
      }
   }

   public Component relationTranslation() {
      return relation.translation(info.getGender());
   }
}
