package com.uncreated.civilized.entity.behaviour;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.schedule.Activity;

public class IdleBehaviourControl extends StatefulBehaviourControl<BehaviourStateMachine> {

   private final Activity activity;

   public IdleBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ShufflingList<BehaviourState> idleTasks) {
      this(Activity.IDLE, tasks, coreTasks, idleTasks);
   }

   public IdleBehaviourControl(
         Activity activity,
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ShufflingList<BehaviourState> idleTasks) {
      super(tasks, coreTasks, idleTasks, GateBehavior.OrderPolicy.SHUFFLED);
      this.activity = activity;
   }

   @Override
   protected BehaviourStateMachine createStateMachine() {
      return new BehaviourStateMachine();
   }

   @Override
   protected boolean canContinueToUse(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {
      return civilizedVillager.getBrain().isActive(activity);
   }
}
