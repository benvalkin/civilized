package com.uncreated.civilized.core.villagerinfo.skills.events;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Hits on hostile mobs train a villager's combat skill. Kills are handled by
 * {@link CivilizedVillager#awardKillScore}, which vanilla calls on the villager itself.
 */
public class SkillTrainingEvents {

   /** Covers melee hits and arrow hits alike: an arrow's damage is credited to whoever shot it. */
   @SubscribeEvent
   public static void onLivingDamaged(LivingDamageEvent.Post event) {
      if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0)
         return;

      if (event.getSource().getEntity() instanceof CivilizedVillager villager && event.getEntity() instanceof Enemy)
         villager.getSkillTraining().gainHitXp();
   }
}
