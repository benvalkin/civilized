package com.uncreated.civilized.core.settlement.defense;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public interface ICombatCommand {

   void reportHostile(CivilizedVillager reporter, LivingEntity hostile);
   Optional<CombatTarget> requestTargetForCombatant(CivilizedVillager combatant);
   boolean shouldContinueToBeDrafted(CivilizedVillager villager);
   boolean shouldContinueToEngageTarget(CivilizedVillager villager, LivingEntity enemy);

   /**
    * Whether a combatant should hold back an attack that might hit {@code entity} by accident, e.g. an arrow passing
    * through it on the way to the actual target.
    */
   boolean shouldAvoidHitting(CivilizedVillager combatant, LivingEntity entity);

   /**
    * Checked whenever the combatant takes damage.
    *
    * @return true if the combatant should run away from the combat engagement to avoid further damage whilst still
    *         remaining drafted.
    */
   boolean shouldRout(CivilizedVillager combatant);

   /**
    * Checked continuously while the combatant is routed.
    *
    * @return true if the routed combatant has recovered enough to return to the combat engagement.
    */
   boolean shouldUnrout(CivilizedVillager combatant);
}
