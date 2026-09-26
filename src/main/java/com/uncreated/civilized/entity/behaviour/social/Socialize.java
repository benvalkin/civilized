package com.uncreated.civilized.entity.behaviour.social;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class Socialize extends StatefulBehaviour {

   private static final double INVITE_RANGE = 12;
   /** How close the villagers stand while they talk. */
   private static final double TALKING_DISTANCE = 2.5;
   /** A partner further away than this has walked off, which ends the conversation. */
   private static final double MAX_PARTNER_DISTANCE = 16;
   /** How long the host has to reach its partner before it gives up. */
   private static final int MAX_TICKS_TO_MEET = 10 * 20;
   /** How long a villager waits after a conversation before starting another one itself. */
   private static final Duration COOLDOWN = Duration.of(20, ChronoUnit.SECONDS);

   private final float speedModifier;

   private long startTime;
   private boolean met;

   public Socialize(float speedModifier) {
      super(BehaviourStates.SOCIALISING, 10 * 20, 90 * 20, 0);
      this.speedModifier = speedModifier;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      // invited by another villager, which is how the partner ends up here
      if (currentConversation(villager).isPresent())
         return true;

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime()))
         return false;

      return tryStartConversation(level, villager);
   }

   /**
    * Invites the closest villager that is free to talk. If nobody accepts, this behaviour doesn't start
    */
   private boolean tryStartConversation(ServerLevel level, CivilizedVillager villager) {
      List<CivilizedVillager> candidates =
            level.getEntitiesOfClass(
                  CivilizedVillager.class,
                  villager.getBoundingBox().inflate(INVITE_RANGE),
                  // TODO: eventually need to filter out hostile villagers
                  other -> other != villager && other.isAlive() && villager.hasLineOfSight(other))
                  .stream()
                  .sorted(Comparator.comparingDouble(villager::distanceToSqr))
                  .toList();

      Conversation conversation = new Conversation(villager, Conversation.PAIR);
      for (CivilizedVillager candidate : candidates) {
         if (candidate.tryJoinConversation(conversation)) {
            villager.getBrain().setMemory(AIRegistry.MM_CONVERSATION.get(), conversation);
            return true;
         }
      }

      return false;
   }

   private static Optional<Conversation> currentConversation(CivilizedVillager villager) {
      return villager.getBrain()
            .getMemory(AIRegistry.MM_CONVERSATION.get())
            .filter(conversation -> conversation.isMember(villager));
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      startTime = gameTime;
      met = false;
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      Optional<CivilizedVillager> partner = currentConversation(villager).flatMap(c -> findPartner(villager, c));
      if (partner.isEmpty()) {
         doStop(level, villager, gameTime);
         return;
      }

      double distance = villager.distanceTo(partner.get());
      if (distance > MAX_PARTNER_DISTANCE || (!met && gameTime - startTime > MAX_TICKS_TO_MEET)) {
         doStop(level, villager, gameTime);
         return;
      }

      // set every tick, because other behaviours (usually inside the Vanilla villager's core behaviours e.g.
      // LookAtTargetSink) may clear the look and walk targets when starting and stopping.
      // It's okay to do this - vanilla villager's Socialize behaviour
      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(partner.get(), true));

      boolean isHost = currentConversation(villager).map(c -> c.getHost() == villager).orElse(false);
      if (distance > TALKING_DISTANCE) {
         // the host walks over, while the villager it invited waits for it
         if (isHost)
            villager.getBrain()
                  .setMemory(
                        MemoryModuleType.WALK_TARGET,
                        new WalkTarget(new EntityTracker(partner.get(), false), speedModifier, 2));
         return;
      }

      met = true;
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

      // // the odd gesture while talking
      // if (villager.getRandom().nextInt(60) == 0)
      // villager.swing(InteractionHand.MAIN_HAND);
   }

   private static Optional<CivilizedVillager> findPartner(CivilizedVillager villager, Conversation conversation) {
      return conversation.othersThan(villager)
            .stream()
            .filter(other -> other.isAlive() && currentConversation(other).filter(c -> c == conversation).isPresent())
            .findFirst();
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);

      // if this villager leaves and there is only one other left in the conversation, the remaining will realize next
      // tick and summarily end the conversation
      villager.getBrain().getMemory(AIRegistry.MM_CONVERSATION.get()).ifPresent(c -> c.leave(villager));
      villager.getBrain().eraseMemory(AIRegistry.MM_CONVERSATION.get());
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);

      getBehaviourCooldowns().startCooldown(Cooldowns.START, COOLDOWN, gameTime);
   }
}
