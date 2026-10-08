package com.uncreated.civilized.entity.behaviour.strike;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.BehaviourStateMachine;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviourControl;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;

public class StrikeBehaviourControl extends StatefulBehaviourControl<BehaviourStateMachine> {

   public StrikeBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ImmutableList<BehaviourState> idleTasks) {
      super(tasks, coreTasks, idleTasks);
   }

   @Override
   protected BehaviourStateMachine createStateMachine() {
      return new BehaviourStateMachine();
   }

   @Override
   protected boolean canContinueToUse(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {
      return civilizedVillager.getBrain().isActive(AIRegistry.A_STRIKE.get());
   }
}
