package com.uncreated.civilized.entity.behaviour;

import java.util.Optional;

import org.apache.commons.lang3.function.TriFunction;

import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MediumDistanceTravelTask {

   private static final int DEFAULT_CHECK_INTERVAL_TICKS = 5 * 20;

   private CivilizedVillager villager;
   private GlobalPos finalDestination;
   private final TriFunction<CivilizedVillager, GlobalPos, Integer, Boolean> closeEnoughTest;
   private float speedModifier;
   private int closeEnoughDistance;
   private long tooFarDistance;
   private long tooLongUnreachableTicks;
   private int checkIntervalTicks = DEFAULT_CHECK_INTERVAL_TICKS;

   private long lastCheckTime;
   @Getter
   private boolean journeySuccessful;

   public MediumDistanceTravelTask(
         CivilizedVillager villager,
         BlockPos finalDestination,
         TriFunction<CivilizedVillager, GlobalPos, Integer, Boolean> closeEnoughTest,
         float speedModifier,
         int closeEnoughDistance,
         long tooFarDistance,
         long tooLongUnreachableTicks) {
      this.villager = villager;
      this.finalDestination = new GlobalPos(villager.level().dimension(), finalDestination);
      this.speedModifier = speedModifier;
      this.closeEnoughTest = closeEnoughTest;
      this.closeEnoughDistance = closeEnoughDistance;
      this.tooFarDistance = tooFarDistance;
      this.tooLongUnreachableTicks = tooLongUnreachableTicks;
   }

   /**
    * How often the walk target is checked and set again. Journeys that need to react quickly, e.g. running away, should
    * use a shorter interval than the default, since a walk target that the villager fails to path to is erased for it
    * by {@code MoveToTargetSink}, leaving it standing still until the next check.
    */
   public MediumDistanceTravelTask checkInterval(int checkIntervalTicks) {
      this.checkIntervalTicks = checkIntervalTicks;
      return this;
   }

   public MediumDistanceTravelTask(CivilizedVillager villager, BlockPos finalDestination) {
      this(villager, finalDestination, manhattanDistTest(), 0.4f, 2, 300, 1500);
   }

   public MediumDistanceTravelTask(CivilizedVillager villager, BlockPos finalDestination, int closeEnoughDistance) {
      this(villager, finalDestination, manhattanDistTest(), 0.4f, closeEnoughDistance, 300, 1500);
   }

   public MediumDistanceTravelTask(
         CivilizedVillager villager,
         BlockPos finalDestination,
         TriFunction<CivilizedVillager, GlobalPos, Integer, Boolean> closeEnoughTest,
         int closeEnoughDistance) {
      this(villager, finalDestination, closeEnoughTest, 0.4f, closeEnoughDistance, 300, 1500);
   }

   private static TriFunction<CivilizedVillager, GlobalPos, Integer, Boolean> manhattanDistTest() {
      return (villager, destination, closeEnoughDistance) -> destination.pos()
            .distManhattan(villager.blockPosition()) <= closeEnoughDistance;
   }

   protected Level getServerLevel() {
      return villager.level();
   }

   protected boolean closeEnoughToPoi() {
      return closeEnoughTest.apply(villager, finalDestination, closeEnoughDistance);
   }

   public void walkToPoi(long gameTicks) {

      if (journeySuccessful)
         return;

      if (closeEnoughToPoi()) {
         journeySuccessful = true;
      }

      if (gameTicks - lastCheckTime < checkIntervalTicks)
         return;

      lastCheckTime = gameTicks;

      Optional<Long> optional = villager.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      if (finalDestination.dimension() == getServerLevel().dimension() && (!optional.isPresent()
            || getServerLevel().getGameTime() - (Long) optional.get() <= tooLongUnreachableTicks)) {
         if (finalDestination.pos().distManhattan(villager.blockPosition()) > tooFarDistance) {
            Vec3 nextIntermediatePos = null;
            int attempts = 0;

            while (nextIntermediatePos == null || BlockPos.containing(nextIntermediatePos)
                  .distManhattan(villager.blockPosition()) > tooFarDistance) {
               nextIntermediatePos =
                     DefaultRandomPos.getPosTowards(
                           villager,
                           15,
                           7,
                           Vec3.atBottomCenterOf(finalDestination.pos()),
                           (float) (Math.PI / 2));
               if (++attempts == 1000) {
                  villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                  villager.getBrain().setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, gameTicks);
                  return;
               }
            }

            villager.getBrain()
                  .setMemory(
                        MemoryModuleType.WALK_TARGET,
                        new WalkTarget(nextIntermediatePos, speedModifier, closeEnoughDistance));
         } else if (!closeEnoughToPoi()) {
            villager.getBrain()
                  .setMemory(
                        MemoryModuleType.WALK_TARGET,
                        new WalkTarget(finalDestination.pos(), speedModifier, closeEnoughDistance));
         }
      } else {
         villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         villager.getBrain().setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, gameTicks);
      }
   }
}
