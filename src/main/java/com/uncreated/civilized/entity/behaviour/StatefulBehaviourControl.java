package com.uncreated.civilized.entity.behaviour;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.CooldownTracker;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.ShufflingList;

public abstract class StatefulBehaviourControl<StateMachine extends BehaviourStateMachine>
      implements BehaviorControl<CivilizedVillager> {

   private final Map<BehaviourState, StatefulBehaviour> tasks;
   private final ImmutableList<BehaviourState> coreTasks;
   private final ShufflingList<BehaviourState> idleTasks;
   private final GateBehavior.OrderPolicy idleTaskOrder;
   StatefulBehaviour currentBehaviour;
   private Behavior.Status status = Behavior.Status.STOPPED;

   private final StateMachine stateMachine;

   private final CooldownTracker<Cooldown> sharedCooldowns;

   public StatefulBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ImmutableList<BehaviourState> idleTasks) {
      this(tasks, coreTasks, equallyWeighted(idleTasks), GateBehavior.OrderPolicy.ORDERED);
   }

   public StatefulBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ShufflingList<BehaviourState> idleTasks,
         GateBehavior.OrderPolicy idleTaskOrder) {
      this.tasks = tasks.stream().collect(Collectors.toMap(StatefulBehaviour::getState, t -> t));
      this.coreTasks = coreTasks;
      this.idleTasks = idleTasks;
      this.idleTaskOrder = idleTaskOrder;
      stateMachine = createStateMachine();
      sharedCooldowns = new CooldownTracker<>();

      for (StatefulBehaviour task : this.tasks.values()) {
         task.setStateMachine(stateMachine);
         task.setSharedCooldowns(sharedCooldowns);
      }
   }

   private static ShufflingList<BehaviourState> equallyWeighted(ImmutableList<BehaviourState> states) {
      ShufflingList<BehaviourState> weighted = new ShufflingList<>();
      states.forEach(state -> weighted.add(state, 1));
      return weighted;
   }

   protected abstract StateMachine createStateMachine();

   @Override
   public Behavior.Status getStatus() {
      return status;
   }

   @Override
   public boolean tryStart(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {
      this.status = Behavior.Status.RUNNING;
      lastIdlePollTicks = currentTicks;
      if (!tryStartNextNonIdleTask(serverLevel, civilizedVillager, currentTicks))
         startNextIdleTask(serverLevel, civilizedVillager, currentTicks);
      return true;
   }

   private long lastIdlePollTicks;

   @Override
   public void tickOrStop(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {

      if (!canContinueToUse(serverLevel, civilizedVillager, currentTicks)) {
         doStop(serverLevel, civilizedVillager, currentTicks);
         return;
      }

      BehaviourState previousState = stateMachine.currentState();

      currentBehaviour.tickOrStop(serverLevel, civilizedVillager, currentTicks);

      if (currentBehaviour.getStatus() == Behavior.Status.STOPPED
            || stateMachine.isIdle() && stateMachine.hasQueuedActions()) {
         Optional<BehaviourState> nextState = stateMachine.pollNextAction();

         if (nextState.isPresent()) {

            StatefulBehaviour task = getTask(nextState.get());
            if (task.tryStart(serverLevel, civilizedVillager, currentTicks)) {
               // a queued action interrupts whatever idle task is running, which then has to be stopped like any task
               // being replaced. Otherwise, its stop() never runs and it is left marked as running
               if (currentBehaviour != task && currentBehaviour.getStatus() != Behavior.Status.STOPPED)
                  currentBehaviour.doStop(serverLevel, civilizedVillager, currentTicks);
               task.lateStart(serverLevel, civilizedVillager, currentTicks);
               stateMachine.currentState(nextState.get());
               currentBehaviour = task;
               stateMachine.isIdle(false);
               return;
            }
         }

         // we only try start the next idle activity if there is no other task running.
         // otherwise, chains of queued actions that fail to start keep resetting the idle task's duration such that it
         // never gets to stop, which ends up blocking other activities.
         if (currentBehaviour.getStatus() == Behavior.Status.STOPPED)
            startNextIdleTask(serverLevel, civilizedVillager, currentTicks);
         return;
      }

      if (stateMachine.isIdle() && currentTicks > lastIdlePollTicks) {
         // idle means we need to periodically attempt new non-idle actions
         tryStartNextNonIdleTask(serverLevel, civilizedVillager, currentTicks);
         lastIdlePollTicks += 5 * 20;
      }
   }

   public Component currentBehaviourDescription() {
      if (status != Behavior.Status.RUNNING || currentBehaviour == null)
         return Component.empty();

      return currentBehaviour.description();
   }

   protected boolean canContinueToUse(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {
      return true;
   }

   private boolean tryStartNextNonIdleTask(
         ServerLevel serverLevel,
         CivilizedVillager civilizedVillager,
         long currentTicks) {
      for (BehaviourState state : coreTasks) {
         StatefulBehaviour task = getTask(state);
         if (task.tryStart(serverLevel, civilizedVillager, currentTicks)) {
            // sanity check that the current task has properly been stopped
            if (currentBehaviour != null && currentBehaviour != task
                  && currentBehaviour.getStatus() != Behavior.Status.STOPPED)
               currentBehaviour.doStop(serverLevel, civilizedVillager, currentTicks);
            task.lateStart(serverLevel, civilizedVillager, currentTicks);
            stateMachine.currentState(task.getState());
            currentBehaviour = task;
            stateMachine.isIdle(false);
            return true;
         }
      }
      return false;
   }

   private void startNextIdleTask(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long currentTicks) {
      // if the order policy is SHUFFLED, apply() randomizes the list of idle tasks in-place.
      // if the order policy is ORDERED, apply() does absolutely nothing, leaving it in the same order that it was
      // supplied in the constructor.
      idleTaskOrder.apply(idleTasks);
      // after this, the next section simply tries to start each activity in the list, which may have been randomized,
      // ultimately resulting in a legitimately random next behaviour
      for (BehaviourState state : idleTasks) {
         StatefulBehaviour task = getTask(state);
         if (task.tryStart(serverLevel, civilizedVillager, currentTicks)) {
            // sanity check that the current task has properly been stopped
            if (currentBehaviour != null && currentBehaviour != task
                  && currentBehaviour.getStatus() != Behavior.Status.STOPPED)
               currentBehaviour.doStop(serverLevel, civilizedVillager, currentTicks);
            task.lateStart(serverLevel, civilizedVillager, currentTicks);
            stateMachine.currentState(task.getState());
            currentBehaviour = task;
            stateMachine.isIdle(true);
            return;
         }
      }
      throw new IllegalStateException(
            "Villager became idle, but was not able to start any idle task. Villagers need at least one idle work state to fall back to.");
   }

   public boolean isRunningIdleTask() {
      return status == Behavior.Status.RUNNING && stateMachine.isIdle();
   }

   public boolean hasTask(BehaviourState state) {
      return tasks.containsKey(state);
   }

   public void queueImmediately(BehaviourState state) {
      stateMachine.queueImmediately(state);
   }

   private StatefulBehaviour getTask(BehaviourState state) {
      @Nullable
      StatefulBehaviour task = tasks.get(state);
      if (task == null)
         throw new IllegalStateException(
               String.format(
                     "Civilized villager has no behaviour associated with the state '%s'. Behaviour could not be started.",
                     state));

      return task;
   }

   @Override
   public void doStop(ServerLevel serverLevel, CivilizedVillager civilizedVillager, long l) {
      this.status = Behavior.Status.STOPPED;
      currentBehaviour.doStop(serverLevel, civilizedVillager, l);
   }

   @Override
   public String debugString() {
      return stateMachine.currentState().toString();
   }
}
