package com.uncreated.civilized.entity.behaviour;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Walks the villager to a random spot inside one of a few buildings.
 */
public abstract class VisitBuilding extends StatefulBehaviour {

   /** How many spots inside a building are tried before giving up on it. */
   private static final int DESTINATION_ATTEMPTS = 16;
   private static final int CLOSE_ENOUGH_DISTANCE = 2;

   private final float speedModifier;
   private final Duration cooldownAfterVisit;
   private final Duration cooldownAfterFailedVisit;

   private BlockPos destination;
   private MediumDistanceTravelTask travelHelper;

   protected VisitBuilding(
         BehaviourState state,
         float speedModifier,
         Duration cooldownAfterVisit,
         Duration cooldownAfterFailedVisit) {
      // long enough to cross a settlement
      super(state, 90 * 20, 0);
      this.speedModifier = speedModifier;
      this.cooldownAfterVisit = cooldownAfterVisit;
      this.cooldownAfterFailedVisit = cooldownAfterFailedVisit;
   }

   /** The buildings the villager could visit, in the order they're tried. Buildings that aren't loaded are skipped. */
   protected abstract List<Building> chooseBuildings(ServerLevel level, CivilizedVillager villager);

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime()))
         return false;

      for (Building building : chooseBuildings(level, villager)) {
         if (LoadedBuildings.checkLoaded(building).isEmpty())
            continue;

         Optional<BlockPos> spot =
               building.getBounds().findRandomStandableSpot(level, villager.getRandom(), DESTINATION_ATTEMPTS);
         if (spot.isPresent()) {
            destination = spot.get();
            return true;
         }
      }

      return false;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper =
            new MediumDistanceTravelTask(
                  villager,
                  destination,
                  (v, target, closeEnough) -> target.pos().distManhattan(v.blockPosition()) <= closeEnough,
                  speedModifier,
                  CLOSE_ENOUGH_DISTANCE,
                  300,
                  1500);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {
      if (travelHelper.isJourneySuccessful()) {
         doStop(level, villager, gameTime);
         return;
      }

      travelHelper.walkToPoi(gameTime);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

      boolean arrived = travelHelper != null && travelHelper.isJourneySuccessful();
      getBehaviourCooldowns()
            .startCooldown(Cooldowns.START, arrived ? cooldownAfterVisit : cooldownAfterFailedVisit, gameTime);
   }
}
