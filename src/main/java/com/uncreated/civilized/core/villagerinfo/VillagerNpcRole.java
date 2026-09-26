package com.uncreated.civilized.core.villagerinfo;

import lombok.Getter;
import net.minecraft.network.chat.Component;

public enum VillagerNpcRole {
   WORKER, TRAVELLER, SUITOR, SPOUSE, MIGRANT, SKILLED_PROFESSIONAL, MERCENARY, BEGGAR, SCOUNDREL, THIEF, MERCHANT, BANDIT, ADVISOR;

   public Component translation() {
      return Component.translatable("civilized.villager.role." + name().toLowerCase());
   }
   public static Component suitorTranslation(Gender gender) {
      return Component.translatable("civilized.villager.role.suitor." + gender.name().toLowerCase());
   }
   public static Component spouseTranslation(Gender gender) {
      return Component.translatable("civilized.villager.role.spouse." + gender.name().toLowerCase());
   }
}
