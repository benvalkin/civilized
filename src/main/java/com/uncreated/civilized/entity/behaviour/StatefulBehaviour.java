package com.uncreated.civilized.entity.behaviour;

import java.util.Map;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.worker.CooldownTracker;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public abstract class StatefulBehaviour extends Behavior<CivilizedVillager> {

   @Getter
   private final BehaviourState state;
   @Getter
   private final int cooldownDuration;

   @Getter
   @Setter(AccessLevel.PACKAGE)
   private BehaviourStateMachine stateMachine;

   @Getter
   private CooldownTracker<Cooldown> behaviourCooldowns;

   @Getter
   @Setter(AccessLevel.PACKAGE)
   private CooldownTracker<Cooldown> sharedCooldowns;

   public StatefulBehaviour(BehaviourState state, int duration, int cooldownDuration) {
      super(Map.of(), duration);
      this.state = state;
      this.cooldownDuration = cooldownDuration;
      this.behaviourCooldowns = new CooldownTracker<>();
      this.sharedCooldowns = new CooldownTracker<>();
   }

   public StatefulBehaviour(BehaviourState state) {
      this(state, 30 * 20, 5 * 20);
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
   }

   /**
    * Due to limitations in how Minecraft's AI Behaviors start and stop, {@link StatefulBehaviourControl} does not
    * guarantee that a currently running behavior stops before another one starts. This can lead to state management
    * problems when one activity's {code stop} method alters the same state as the {@code start} method of the behavior
    * is queued next. This method is guaranteed to run after the previously running behavior's {@code stop} method is
    * called.
    */
   protected void lateStart(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   /**
    * Creates a unique reservation party name for the specified villager using its villagerId and this work behaviour's
    * {@link BehaviourState}. Used for reserving items during logistics.
    */
   protected String reservationPartyKey(CivilizedVillager villager) {
      return villager.getInfo().getVillagerId() + ":" + getState();
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      return true;
   }
}
