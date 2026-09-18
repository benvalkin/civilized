package com.uncreated.civilized.entity.behaviour;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

/**
 * Walks the villager home and puts it to sleep in a free bed. If every bed in the home is taken, it sleeps on the floor
 * somewhere inside instead. A villager that gets stuck on the way is teleported to its spot, so that nobody ends up
 * spending the night outside because of bad pathfinding.
 */
public class SleepInBed extends Behavior<CivilizedVillager> {

   private static final int STUCK_TICKS_BEFORE_TELEPORTING = 10 * 20;
   /** How much closer the villager has to get to its spot to count as making progress, in blocks. */
   private static final double MIN_PROGRESS_DISTANCE = 0.5;
   private static final int CLOSE_ENOUGH_DISTANCE = 2;

   /**
    * Sleeping spots that villagers are heading to or sleeping in. Beds are only marked as occupied once someone is
    * actually in them, so without this, two villagers could pick the same bed and race each other to it.
    */
   private static final Map<GlobalPos, CivilizedVillager> CLAIMED_SPOTS = new HashMap<>();

   /**
    * @param pos
    *           the bed's head, or where the villager's head lies on the floor
    * @param floorHeadDirection
    *           for floor spots, the direction from the villager's feet to its head
    */
   private record SleepingSpot(BlockPos pos, boolean isBed, @Nullable Direction floorHeadDirection) {

      static SleepingSpot bed(BlockPos pos) {
         return new SleepingSpot(pos, true, null);
      }

      static SleepingSpot floor(BlockPos head, Direction headDirection) {
         return new SleepingSpot(head, false, headDirection);
      }

      /** Every block the villager's body takes up while sleeping here. */
      List<BlockPos> occupiedBlocks() {
         if (isBed)
            return List.of(pos);
         return List.of(pos, pos.relative(floorHeadDirection.getOpposite()));
      }
   }

   private final float speedModifier;

   private @Nullable SleepingSpot sleepingSpot;
   private @Nullable Building home;
   private MediumDistanceTravelTask travelHelper;
   private double closestDistanceSoFar;
   private long lastProgressTime;

   public SleepInBed(float speedModifier) {
      super(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT), Integer.MAX_VALUE);
      this.speedModifier = speedModifier;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      Optional<LoadedBuilding> loadedHome = LoadedBuildings.checkLoaded(villager.getInfo().getHomeBuildingId());
      if (loadedHome.isEmpty())
         return false;

      home = loadedHome.get().getBuilding();
      sleepingSpot = findSleepingSpot(level, villager).orElse(null);
      return sleepingSpot != null;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      goToSpot(level, villager, sleepingSpot, gameTime);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getBrain().isActive(Activity.REST);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      if (villager.isSleeping())
         villager.stopSleeping();

      if (sleepingSpot != null)
         releaseClaim(level, villager, sleepingSpot);

      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      sleepingSpot = null;
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (villager.isSleeping())
         return;

      // someone else, e.g. a player, may have taken the bed while the villager was on its way
      if (sleepingSpot.isBed() && !isFreeBed(level, villager, sleepingSpot.pos())) {
         Optional<SleepingSpot> newSpot = findSleepingSpot(level, villager);
         if (newSpot.isEmpty())
            return;

         goToSpot(level, villager, newSpot.get(), gameTime);
      }

      if (hasArrived(villager)) {
         sleep(villager);
         return;
      }

      travelHelper.walkToPoi(gameTime);

      double distance = villager.position().distanceTo(Vec3.atBottomCenterOf(sleepingSpot.pos()));
      if (distance < closestDistanceSoFar - MIN_PROGRESS_DISTANCE) {
         closestDistanceSoFar = distance;
         lastProgressTime = gameTime;
      } else if (gameTime - lastProgressTime >= STUCK_TICKS_BEFORE_TELEPORTING) {
         Vec3 spot = Vec3.atBottomCenterOf(sleepingSpot.pos());
         villager.teleportTo(spot.x, spot.y, spot.z);
         sleep(villager);
      }
   }

   private void goToSpot(ServerLevel level, CivilizedVillager villager, SleepingSpot spot, long gameTime) {
      if (sleepingSpot != null)
         releaseClaim(level, villager, sleepingSpot);

      sleepingSpot = spot;
      for (BlockPos pos : spot.occupiedBlocks())
         CLAIMED_SPOTS.put(GlobalPos.of(level.dimension(), pos), villager);

      travelHelper =
            new MediumDistanceTravelTask(
                  villager,
                  spot.pos(),
                  (v, destination, closeEnough) -> destination.pos().distManhattan(v.blockPosition()) <= closeEnough,
                  speedModifier,
                  CLOSE_ENOUGH_DISTANCE,
                  300,
                  1500);
      closestDistanceSoFar = Double.MAX_VALUE;
      lastProgressTime = gameTime;
   }

   private boolean hasArrived(CivilizedVillager villager) {
      // being inside the home as well stops the villager from getting into bed through a wall
      return sleepingSpot.pos().distManhattan(villager.blockPosition()) <= CLOSE_ENOUGH_DISTANCE
            && home.getBounds().contains(villager.blockPosition());
   }

   private void sleep(CivilizedVillager villager) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      villager.getNavigation().stop();

      if (sleepingSpot.isBed())
         villager.startSleeping(sleepingSpot.pos());
      else
         villager.sleepOnFloor(sleepingSpot.pos(), sleepingSpot.floorHeadDirection());
   }

   /** The closest free bed in the villager's home, or a random spot on the floor if there isn't one. */
   private Optional<SleepingSpot> findSleepingSpot(ServerLevel level, CivilizedVillager villager) {
      List<BlockPos> freeBeds = Lists.newArrayList();
      home.getBounds().traverseBlocksWithin(traversal -> {
         BlockPos pos = traversal.getCurrentBlockPos();
         if (isFreeBed(level, villager, pos))
            freeBeds.add(pos.immutable());
      });

      Optional<BlockPos> closestBed =
            freeBeds.stream().min(Comparator.comparingDouble(a -> a.distToCenterSqr(villager.position())));
      if (closestBed.isPresent())
         return Optional.of(SleepingSpot.bed(closestBed.get()));

      // a villager lying on the floor takes up two blocks, like a bed does, so both have to be clear floor with nothing
      // in the way, or the villager would lie halfway into a wall or door
      Set<BlockPos> floor = home.getBounds().findValidInsideFloorBlocks(level);
      List<SleepingSpot> freeFloorSpots = Lists.newArrayList();
      for (BlockPos head : floor) {
         for (Direction headDirection : Direction.Plane.HORIZONTAL) {
            SleepingSpot spot = SleepingSpot.floor(head, headDirection);
            boolean isClear =
                  spot.occupiedBlocks()
                        .stream()
                        .allMatch(pos -> floor.contains(pos) && !isClaimedByAnotherVillager(level, villager, pos));
            if (isClear)
               freeFloorSpots.add(spot);
         }
      }

      if (freeFloorSpots.isEmpty())
         return Optional.empty();

      return Optional.of(freeFloorSpots.get(villager.getRandom().nextInt(freeFloorSpots.size())));
   }

   private void releaseClaim(ServerLevel level, CivilizedVillager villager, SleepingSpot spot) {
      for (BlockPos pos : spot.occupiedBlocks())
         CLAIMED_SPOTS.remove(GlobalPos.of(level.dimension(), pos), villager);
   }

   private boolean isFreeBed(ServerLevel level, CivilizedVillager villager, BlockPos pos) {
      BlockState state = level.getBlockState(pos);

      // sleeping is always done from the head of the bed
      return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD
            && !state.getValue(BedBlock.OCCUPIED) && !isClaimedByAnotherVillager(level, villager, pos);
   }

   private boolean isClaimedByAnotherVillager(ServerLevel level, CivilizedVillager villager, BlockPos pos) {
      CivilizedVillager claimant = CLAIMED_SPOTS.get(GlobalPos.of(level.dimension(), pos));
      // dead or unloaded villagers may not have had the chance to give up their claim
      return claimant != null && claimant != villager && claimant.isAlive();
   }
}
