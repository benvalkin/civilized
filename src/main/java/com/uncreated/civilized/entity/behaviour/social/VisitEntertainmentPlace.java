package com.uncreated.civilized.entity.behaviour.social;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class VisitEntertainmentPlace extends StatefulBehaviour {

   private static final int TAVERN_WEIGHT = 6;
   private static final int TOWN_SQUARE_WEIGHT = 2;
   private static final int OTHER_FOOD_VENDOR_WEIGHT = 2;

   private static final Duration COOLDOWN_AFTER_VISIT = Duration.of(5, ChronoUnit.MINUTES);
   private static final Duration COOLDOWN_AFTER_FAILED_VISIT = Duration.of(30, ChronoUnit.SECONDS);

   /** How many spots inside a building are tried before giving up on it. */
   private static final int DESTINATION_ATTEMPTS = 16;
   private static final int CLOSE_ENOUGH_DISTANCE = 2;

   private final float speedModifier;

   private BlockPos destination;
   private MediumDistanceTravelTask travelHelper;

   public VisitEntertainmentPlace(float speedModifier) {
      // long enough to cross a settlement
      super(BehaviourStates.VISITING_ENTERTAINMENT, 90 * 20, 0);
      this.speedModifier = speedModifier;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime()))
         return false;

      Optional<BlockPos> chosen = chooseDestination(level, villager);
      chosen.ifPresent(pos -> destination = pos);
      return chosen.isPresent();
   }

   private Optional<BlockPos> chooseDestination(ServerLevel level, CivilizedVillager villager) {
      ShufflingList<Building> places = new ShufflingList<>();
      for (Building building : ServerBuildingsStore.INSTANCE.findForSettlement(villager.getInfo().getSettlementId())) {
         int weight = entertainmentWeight(building.getBuildingType());
         if (weight > 0 && LoadedBuildings.checkLoaded(building).isPresent())
            places.add(building, weight);
      }

      for (Building place : places.shuffle()) {
         Optional<BlockPos> spot =
               place.getBounds().findRandomStandableSpot(level, villager.getRandom(), DESTINATION_ATTEMPTS);
         if (spot.isPresent())
            return spot;
      }

      return Optional.empty();
   }

   private static int entertainmentWeight(BuildingType type) {
      // checked first, since a tavern is also a food vendor
      if (type.is(BuildingTypes.TAVERN))
         return TAVERN_WEIGHT;
      if (type.is(BuildingTypes.TOWN_SQUARE))
         return TOWN_SQUARE_WEIGHT;
      if (type.isFoodVendor())
         return OTHER_FOOD_VENDOR_WEIGHT;
      return 0;
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
            .startCooldown(Cooldowns.START, arrived ? COOLDOWN_AFTER_VISIT : COOLDOWN_AFTER_FAILED_VISIT, gameTime);
   }
}
