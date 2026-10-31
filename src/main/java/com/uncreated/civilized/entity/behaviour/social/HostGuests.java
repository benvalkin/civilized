package com.uncreated.civilized.entity.behaviour.social;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.Cooldowns;

import net.minecraft.server.level.ServerLevel;

public class HostGuests extends Socialize {

   private static final Duration COOLDOWN = Duration.of(60, ChronoUnit.SECONDS);

   private static final double INVITE_RANGE = 24;

   public HostGuests(float speedModifier) {
      super(BehaviourStates.HOSTING_GUESTS, speedModifier, 20 * 20, 40 * 20, 0);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      // only homemakers can run this behaviour
      if (!villager.getInfo().isHomemaker())
         return false;

      if (currentConversation(villager).isPresent())
         return false;

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime()))
         return false;

      return tryStartConversation(level, villager);
   }

   @Override
   protected @NotNull List<CivilizedVillager> getCandidateConversationMembers(
         ServerLevel level,
         CivilizedVillager villager) {
      return level
            .getEntitiesOfClass(
                  CivilizedVillager.class,
                  villager.getBoundingBox().inflate(INVITE_RANGE),
                  other -> other != villager && other.isAlive() && !other.isDrafted()
                        && VisitorBehaviour.of(other).isPresent())
            .stream()
            .sorted(Comparator.comparingDouble(villager::distanceToSqr))
            .toList();
   }

   @Override
   protected Duration getRestartCooldown() {
      return COOLDOWN;
   }
}
