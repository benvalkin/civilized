package com.uncreated.civilized.entity.behaviour;

import javax.annotation.Nullable;

import org.apache.commons.lang3.function.TriFunction;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.pathfinding.PathfindingBudget;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Walks a villager to a destination that can be up to a few hundred blocks away (possibly further). This is done by
 * splitting the journey up into short legs. Each leg is a short path search towards the destination, which is cheap and
 * rarely fails, and serves as a heuristic for getting to the actual final destination. This approach was used to keep
 * pathfinding performance costs low whilst supporting travelling over medium-to-long distances.
 * <p>
 * A {@link MediumDistanceTravelTask} drives the villager's navigation directly by setting its
 * {@link MemoryModuleType#PATH} memory, as opposed to simply setting its {@link MemoryModuleType#WALK_TARGET} memory.
 * The main reason for this is that vanilla's {@code MoveToTargetSink} behaviour, which is responsible for driving
 * villagers along path movement, erases the WALK_TARGET memory whenever a path cannot be found. This can cause the
 * villager to stand still unless a new valid path is promptly calculated. Additional reasons include i) being able to
 * limit the number of {@link CivilizedVillager} pathfinding attempts that can occur across the entire server (another
 * performance optimization), ii) pathfinding performance telemetry, and iii) general fine control over pathing
 * decisions.
 * <p>
 * When a leg stops getting the villager any closer, a detour is tried instead. A detour is another (less intelligent)
 * leg that simply alternates between a random spot towards the destination and a random spot in any direction to back
 * out of dead ends. Repeated failures wait longer and longer before trying again, so that an unreachable destination
 * doesn't search for a path every tick.
 * <p>
 * To make the vilalger move along the path, {@link #walkToPoi} must be called every tick while travelling. If it stops
 * being called, e.g. because the behaviour using it stopped, the villager stops walking.
 */
public class MediumDistanceTravelTask {

   /** Journeys longer than this are split into legs. */
   private static final int MAX_LEG_LENGTH = 28;
   /** The path accuracy to use when pathfinding to intermediate legs. */
   private static final int LEG_ACCURACY = 2;
   /** The path accuracy to use when pathfinding to the actual destination (during the last leg). */
   private static final int FINAL_LEG_MAX_ACCURACY = 2;
   /** How close to the end of its leg a villager has to get in that time to count as making progress. */
   private static final double MIN_PROGRESS = 1;
   private static final int DEFAULT_STALL_TICKS = 2 * 20;
   private static final int MIN_RETRY_DELAY_TICKS = 5;
   private static final int MAX_RETRY_DELAY_TICKS = 5 * 20;
   private static final int DETOUR_RANGE = 16;
   private static final int DETOUR_Y_RANGE = 7;
   /** How many ticks {@link #walkToPoi} can go without being called before the journey counts as abandoned. */
   private static final int ABANDONED_AFTER_TICKS = 2;
   private static final float DEFAULT_SPEED = 0.4f;
   private static final long DEFAULT_GIVE_UP_TICKS = 1500;

   private final CivilizedVillager villager;
   private final GlobalPos finalDestination;
   private final TriFunction<CivilizedVillager, GlobalPos, Integer, Boolean> closeEnoughTest;
   private final float speedModifier;
   private final int closeEnoughDistance;
   private final int legLength;
   private final long giveUpTicks;
   private int stallTicks = DEFAULT_STALL_TICKS;

   @Getter
   private boolean journeySuccessful;
   /**
    * Whether the villager has gone too long without getting any closer to their destination. They keep trying, but only
    * every few seconds, so it's up to the caller whether to give up on the journey.
    */
   @Getter
   private boolean journeyFailed;

   @Nullable
   private Path legPath;
   private boolean legIsDetour;
   private Vec3 legStartPos;
   private double legBestDistance;
   private long legProgressTime;
   private double distanceAtLegStart;

   private double journeyBestDistance = Double.MAX_VALUE;
   private long journeyProgressTime = -1;

   /** How many leg attempts in a row that haven't got the villager any closer to the destination. */
   private int failedAttempts;
   private boolean nextLegIsDetour;
   private long nextAttemptTime;
   private long lastDrivenTime;

   /**
    * @param maxLegLength
    *           the longest leg the journey can be split into. Capped at {@value #MAX_LEG_LENGTH}
    * @param giveUpTicks
    *           how long the villager can go without getting any closer before the journey counts as failed
    */
   public MediumDistanceTravelTask(
         CivilizedVillager villager,
         BlockPos finalDestination,
         TriFunction<CivilizedVillager, GlobalPos, Integer, Boolean> closeEnoughTest,
         float speedModifier,
         int closeEnoughDistance,
         int maxLegLength,
         long giveUpTicks) {
      this.villager = villager;
      this.finalDestination = new GlobalPos(villager.level().dimension(), finalDestination);
      this.speedModifier = speedModifier;
      this.closeEnoughTest = closeEnoughTest;
      this.closeEnoughDistance = closeEnoughDistance;
      this.legLength = Math.clamp(maxLegLength, LEG_ACCURACY * 2, MAX_LEG_LENGTH);
      this.giveUpTicks = giveUpTicks;
   }

   public MediumDistanceTravelTask(CivilizedVillager villager, BlockPos finalDestination) {
      this(villager, finalDestination, manhattanDistTest(), DEFAULT_SPEED, 2, MAX_LEG_LENGTH, DEFAULT_GIVE_UP_TICKS);
   }

   public MediumDistanceTravelTask(CivilizedVillager villager, BlockPos finalDestination, int closeEnoughDistance) {
      this(
            villager,
            finalDestination,
            manhattanDistTest(),
            DEFAULT_SPEED,
            closeEnoughDistance,
            MAX_LEG_LENGTH,
            DEFAULT_GIVE_UP_TICKS);
   }

   public MediumDistanceTravelTask(
         CivilizedVillager villager,
         BlockPos finalDestination,
         TriFunction<CivilizedVillager, GlobalPos, Integer, Boolean> closeEnoughTest,
         int closeEnoughDistance) {
      this(
            villager,
            finalDestination,
            closeEnoughTest,
            DEFAULT_SPEED,
            closeEnoughDistance,
            MAX_LEG_LENGTH,
            DEFAULT_GIVE_UP_TICKS);
   }

   /**
    * How long a movement leg can go on without getting the villager closer to the leg's destination before a detour is
    * attempted. Journeys that need to react quickly, e.g. running away, can use a shorter time than the default.
    */
   public MediumDistanceTravelTask stallTicks(int stallTicks) {
      this.stallTicks = stallTicks;
      return this;
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

      lastDrivenTime = gameTicks;
      villager.setActiveTravel(this);

      if (finalDestination.dimension() != getServerLevel().dimension())
         return;

      if (closeEnoughToPoi()) {
         journeySuccessful = true;
         stopLeg();
         villager.clearActiveTravel(this);
         return;
      }

      trackJourneyProgress(gameTicks);

      if (legPath != null) {
         if (currentLegIsMakingProgress(legPath, gameTicks))
            return;

         finishLeg(gameTicks);
      }

      if (gameTicks < nextAttemptTime)
         return;

      // same as vanilla, villagers cannot plan a path while in the air, e.g. jumping up a block
      if (!villager.onGround() && !villager.isInLiquid())
         return;

      if (!PathfindingBudget.INSTANCE.tryAcquire(gameTicks))
         return;

      startNextLeg(gameTicks);
   }

   /** Whether {@link #walkToPoi} has stopped being called, meaning whatever was using it has moved on. */
   public boolean isAbandoned(long gameTicks) {
      return gameTicks - lastDrivenTime > ABANDONED_AFTER_TICKS;
   }

   /** Stops the villager following this journey's path. Walking resumes if {@link #walkToPoi} is called again. */
   public void abandon() {
      stopLeg();
   }

   private void trackJourneyProgress(long gameTicks) {
      double distance = distanceToDestination();
      if (journeyProgressTime < 0 || distance < journeyBestDistance - MIN_PROGRESS) {
         journeyBestDistance = distance;
         journeyProgressTime = gameTicks;
         journeyFailed = false;
         failedAttempts = 0;
         return;
      }

      if (gameTicks - journeyProgressTime > giveUpTicks)
         journeyFailed = true;
   }

   private double distanceToDestination() {
      return villager.position().distanceTo(Vec3.atBottomCenterOf(finalDestination.pos()));
   }

   /** Whether the villager is still following its current travel leg and is getting closer to the end of it. */
   private boolean currentLegIsMakingProgress(Path leg, long gameTicks) {
      PathNavigation navigation = villager.getNavigation();
      // the navigation drops the path once it's been walked, or if vanilla thinks the villager is stuck
      if (navigation.getPath() != leg || leg.isDone())
         return false;

      double distance = distanceToLegEnd(leg);
      if (distance < legBestDistance - MIN_PROGRESS) {
         legBestDistance = distance;
         legProgressTime = gameTicks;
         return true;
      }

      return gameTicks - legProgressTime <= stallTicks;
   }

   private double distanceToLegEnd(Path path) {
      Node end = path.getEndNode();
      if (end == null)
         return 0;

      return villager.position().distanceTo(Vec3.atBottomCenterOf(end.asBlockPos()));
   }

   private void finishLeg(long gameTicks) {
      boolean madeProgress = distanceToDestination() < distanceAtLegStart - MIN_PROGRESS;
      boolean walkedDetour = legIsDetour && villager.position().distanceTo(legStartPos) > MIN_PROGRESS;
      stopLeg();
      if (madeProgress) {
         nextLegIsDetour = false;
         nextAttemptTime = gameTicks;
      } else if (walkedDetour) {
         // try heading straight for the destination again from wherever the detour ended up
         nextLegIsDetour = false;
         nextAttemptTime = gameTicks + retryDelay();
      } else {
         attemptFailed(gameTicks);
      }
   }

   private void attemptFailed(long gameTicks) {
      failedAttempts++;
      nextLegIsDetour = true;
      nextAttemptTime = gameTicks + retryDelay();
   }

   private int retryDelay() {
      // the first retry is straight away, since it tries a detour rather than the same leg again
      if (failedAttempts <= 1)
         return 0;

      int delay = MIN_RETRY_DELAY_TICKS << Math.min(failedAttempts - 2, 5);
      return Math.min(delay, MAX_RETRY_DELAY_TICKS);
   }

   private void startNextLeg(long gameTicks) {
      boolean detour = nextLegIsDetour;
      BlockPos legTarget;
      int accuracy = LEG_ACCURACY;
      if (detour) {
         legTarget = findDetour();
      } else if (distanceToDestination() <= legLength) {
         legTarget = finalDestination.pos();
         accuracy = Math.min(closeEnoughDistance, FINAL_LEG_MAX_ACCURACY);
      } else {
         legTarget = findPartialLegTowardsDestination();
      }

      if (legTarget == null) {
         attemptFailed(gameTicks);
         return;
      }

      PathNavigation navigation = villager.getNavigation();
      long searchStart = System.nanoTime();
      Path path = navigation.createPath(legTarget, accuracy);
      boolean usable = path != null && pathMakesProgress(path, detour);
      PathfindingBudget.INSTANCE.recordSearch(System.nanoTime() - searchStart, !usable);

      if (!usable || !navigation.moveTo(path, speedModifier)) {
         attemptFailed(gameTicks);
         return;
      }

      // a leftover walk target would get MoveToTargetSink fighting over the navigation
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      // the navigation keeps its old path object if the new one is the same, so use whichever it ended up with
      legPath = navigation.getPath();
      legIsDetour = detour;
      legStartPos = villager.position();
      // doors along the way are opened by InteractWithDoorAndGate, which reads the path from here
      villager.getBrain().setMemory(MemoryModuleType.PATH, legPath);
      legBestDistance = distanceToLegEnd(legPath);
      legProgressTime = gameTicks;
      distanceAtLegStart = distanceToDestination();
   }

   private boolean pathMakesProgress(Path path, boolean detour) {
      Node end = path.getEndNode();
      if (end == null)
         return false;

      Vec3 endPos = Vec3.atBottomCenterOf(end.asBlockPos());
      // a detour is allowed to head away from the destination, as long as it goes somewhere
      if (detour)
         return endPos.distanceTo(villager.position()) > MIN_PROGRESS;

      if (path.canReach())
         return true;

      // a partial path still helps as long as it ends up closer
      Vec3 destination = Vec3.atBottomCenterOf(finalDestination.pos());
      return endPos.distanceTo(destination) < distanceToDestination() - MIN_PROGRESS;
   }

   private BlockPos findPartialLegTowardsDestination() {
      Vec3 from = villager.position();
      Vec3 to = Vec3.atBottomCenterOf(finalDestination.pos());
      double fraction = legLength / from.distanceTo(to);
      Vec3 target = from.lerp(to, fraction);
      BlockPos targetPos = BlockPos.containing(target);

      // outdoors, aim for the ground rather than wherever the straight line happens to be, which could be inside a hill
      Level level = getServerLevel();
      if (level.canSeeSky(villager.blockPosition()) && level.hasChunkAt(targetPos)) {
         int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());
         return new BlockPos(targetPos.getX(), groundY, targetPos.getZ());
      }

      return targetPos;
   }

   private @Nullable BlockPos findDetour() {
      // alternate between going around whatever's in the way, and backing out of what might be a dead end
      Vec3 detour;
      if (failedAttempts % 2 == 1)
         detour =
               LandRandomPos.getPosTowards(
                     villager,
                     DETOUR_RANGE,
                     DETOUR_Y_RANGE,
                     Vec3.atBottomCenterOf(finalDestination.pos()));
      else
         detour = LandRandomPos.getPos(villager, DETOUR_RANGE, DETOUR_Y_RANGE);

      if (detour == null)
         return null;

      return BlockPos.containing(detour);
   }

   private void stopLeg() {
      if (legPath == null)
         return;

      if (villager.getNavigation().getPath() == legPath)
         villager.getNavigation().stop();

      if (villager.getBrain().getMemory(MemoryModuleType.PATH).orElse(null) == legPath)
         villager.getBrain().eraseMemory(MemoryModuleType.PATH);

      legPath = null;
   }
}
