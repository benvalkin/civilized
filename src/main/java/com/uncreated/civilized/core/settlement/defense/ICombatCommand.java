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
}
