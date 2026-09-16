package com.uncreated.civilized.entity.behaviour;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.worker.CooldownTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

public abstract class StatefulBehaviourControl<StateMachine extends BehaviourStateMachine>
      implements BehaviorControl<CivilizedVillager> {

   private final Map<BehaviourState, StatefulBehaviour> tasks;
   private final ImmutableList<BehaviourState> coreTasks;
   private final ImmutableList<BehaviourState> idleTasks;
   StatefulBehaviour currentBehaviour;
   private Behavior.Status status = Behavior.Status.STOPPED;

   private final StateMachine stateMachine;

   private final CooldownTracker<Cooldown> sharedCooldowns;

   public StatefulBehaviourControl(
         ImmutableList<StatefulBehaviour> tasks,
         ImmutableList<BehaviourState> coreTasks,
         ImmutableList<BehaviourState> idleTasks) {
      this.tasks = tasks.stream().collect(Collectors.toMap(StatefulBehaviour::getState, t -> t));
      this.coreTasks = coreTasks;
      this.idleTasks = idleTasks;
      stateMachine = createStateMachine();
      sharedCooldowns = new CooldownTracker<>();

      for (StatefulBehaviour task : this.tasks.values()) {
         task.setStateMachine(stateMachine);
         task.setSharedCooldowns(sharedCooldowns);
      }
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
               task.lateStart(serverLevel, civilizedVillager, currentTicks);
               stateMachine.currentState(nextState.get());
               currentBehaviour = task;
               stateMachine.isIdle(false);
               return;
            }
         }
         startNextIdleTask(serverLevel, civilizedVillager, currentTicks);
         return;
      }

      if (stateMachine.isIdle() && currentTicks > lastIdlePollTicks) {
         // idle means we need to periodically attempt new non-idle actions
         tryStartNextNonIdleTask(serverLevel, civilizedVillager, currentTicks);
         lastIdlePollTicks += 5 * 20;
      }
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
