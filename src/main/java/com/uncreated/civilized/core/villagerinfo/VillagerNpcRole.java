package com.uncreated.civilized.core.villagerinfo;

import net.minecraft.network.chat.Component;

public enum VillagerNpcRole {
   WORKER, TRAVELLER, MIGRANT, SKILLED_PROFESSIONAL, MERCENARY, BEGGAR, SCOUNDREL, THIEF, MERCHANT, BANDIT, ADVISOR;

   public Component translation() {
      return Component.translatable("villager.occupation." + name().toLowerCase());
   }
}
