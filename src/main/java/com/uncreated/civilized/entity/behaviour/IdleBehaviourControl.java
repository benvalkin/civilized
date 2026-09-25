package com.uncreated.civilized.entity.behaviour;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.schedule.Activity;

public class IdleBehaviourControl extends StatefulBehaviourControl<BehaviourStateMachine> {

   public IdleBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ShufflingList<BehaviourState> idleTasks) {
      super(tasks, coreTasks, idleTasks, GateBehavior.OrderPolicy.SHUFFLED);
   }

   @Override
   protected BehaviourStateMachine createStateMachine() {
      return new BehaviourStateMachine();
   }

   @Override
   protected boolean canContinueToUse(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {
      return civilizedVillager.getBrain().isActive(Activity.IDLE);
   }
}
