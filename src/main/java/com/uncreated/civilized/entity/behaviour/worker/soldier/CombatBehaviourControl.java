package com.uncreated.civilized.entity.behaviour.worker.soldier;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviourControl;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;

public class CombatBehaviourControl extends StatefulBehaviourControl<CombatStateMachine> {
   public CombatBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ImmutableList<BehaviourState> idleTasks) {
      super(tasks, coreTasks, idleTasks);
   }

   @Override
   public CombatStateMachine createStateMachine() {
      return new CombatStateMachine();
   }

   @Override
   protected boolean canContinueToUse(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {
      return civilizedVillager.getBrain().isActive(AIRegistry.A_DRAFTED.get());
   }
}
