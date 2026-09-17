package com.uncreated.civilized.entity.behaviour.worker;

import static com.uncreated.civilized.entity.behaviour.CivilizedVillagerActivities.getMinimalLookBehavior;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.IdleStrollAroundSettlement;
import com.uncreated.civilized.entity.behaviour.worker.soldier.BowAttackTarget;
import com.uncreated.civilized.entity.behaviour.worker.soldier.CombatBehaviourControl;
import com.uncreated.civilized.entity.behaviour.worker.soldier.CombatStates;
import com.uncreated.civilized.entity.behaviour.worker.soldier.MeleeAttackTarget;

import net.minecraft.world.entity.ai.behavior.BehaviorControl;

public class CombatActivities {
   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getCombatPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new CombatBehaviourControl(
                        ImmutableList.of(
                              new BowAttackTarget(CombatStates.BOW_ATTACK_TARGET, 0.5f, 15f, 30),
                              new MeleeAttackTarget(CombatStates.MELEE_ATTACK_TARGET, 0.5f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(CombatStates.BOW_ATTACK_TARGET, CombatStates.MELEE_ATTACK_TARGET),
                        ImmutableList.of(BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }
}
