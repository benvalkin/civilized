package com.uncreated.civilized.entity.behaviour;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;

/**
 * Runs away from combat while the villager is routed, i.e. its {@link Activity#PANIC} activity is active (see
 * {@link CivilizedVillager#isRouted()}), until {@link CivilizedVillager#shouldUnrout()} says it can stop. The villager
 * heads straight for the nearest building of its settlement to hide in. Only when there is nowhere to hide does it run
 * away from the nearest hostile instead.
 */
public class Routed extends Behavior<CivilizedVillager> {

   /**
    * Villagers further than this from their settlement's origin won't try to get back to hide.
    */
   private static final double MAX_DISTANCE_FROM_SETTLEMENT_TO_HIDE = 200;
   private static final int FLEE_RADIUS = 16;
   private static final int FLEE_Y_RANGE = 7;
   /**
    * How often a new flee position is picked, to account for the hostile moving.
    */
   private static final int FLEE_REPATH_INTERVAL = 40;
   /**
    * Travel to hiding spots in hops of about this many blocks, so pathfinding stays within follow range.
    */
   private static final int HIDE_TRAVEL_HOP_DISTANCE = 32;
   /**
    * How often the nearest hiding spot is searched for again if none was found.
    */
   private static final int HIDING_SPOT_SEARCH_INTERVAL = 2 * 20;
   /**
    * How often the walk target towards a hiding spot is set again. Kept short so that a failed path doesn't leave the
    * villager standing still.
    */
   private static final int HIDE_TRAVEL_CHECK_INTERVAL = 10;

   private final float moveSpeed;

   public Routed(float moveSpeed) {
      super(Map.of(), Integer.MAX_VALUE);
      this.moveSpeed = moveSpeed;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      return villager.isRouted();
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.isRouted();
   }

   private boolean fleeing;
   private long nextFleeRepath;
   @Nullable
   private BlockPos hidingSpot;
   private long nextHidingSpotSearch;
   @Nullable
   private MediumDistanceTravelTask travelToHidingSpot;

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      // a stale failure from whatever the villager was doing before would otherwise hold up its escape
      villager.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      fleeing = false;
      nextFleeRepath = 0;
      hidingSpot = null;
      nextHidingSpotSearch = 0;
      travelToHidingSpot = null;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long currentTicks) {

      if (villager.shouldUnrout()) {
         villager.unrout(); // switches away from the panic activity, so canStillUse will stop the behaviour next tick
         return;
      }

      if (hidingSpot == null && canHideInSettlement(villager) && currentTicks >= nextHidingSpotSearch) {
         nextHidingSpotSearch = currentTicks + HIDING_SPOT_SEARCH_INTERVAL;
         hidingSpot = findHidingSpot(level, villager).orElse(null);
      }

      if (hidingSpot == null) {
         // nowhere to hide, so just run away from the nearest hostile instead
         villager.getBrain()
               .getMemory(MemoryModuleType.NEAREST_HOSTILE)
               .filter(LivingEntity::isAlive)
               .ifPresent(hostile -> fleeFrom(villager, hostile, currentTicks));
         return;
      }

      if (fleeing) {
         // was running away before a hiding spot was found, so the walk target is still pointing somewhere random
         fleeing = false;
         villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         travelToHidingSpot = null;
      }

      if (travelToHidingSpot == null) {
         travelToHidingSpot =
               new MediumDistanceTravelTask(
                     villager,
                     hidingSpot,
                     (v, destination, closeEnough) -> destination.pos().distManhattan(v.blockPosition()) <= closeEnough,
                     moveSpeed,
                     1,
                     HIDE_TRAVEL_HOP_DISTANCE,
                     30 * 20).checkInterval(HIDE_TRAVEL_CHECK_INTERVAL);
      }

      travelToHidingSpot.walkToPoi(currentTicks);
   }

   private void fleeFrom(CivilizedVillager villager, LivingEntity hostile, long currentTicks) {
      boolean needsNewFleePos =
            !fleeing || currentTicks >= nextFleeRepath
                  || !villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
      fleeing = true;

      if (!needsNewFleePos)
         return;

      nextFleeRepath = currentTicks + FLEE_REPATH_INTERVAL;

      Vec3 fleePos = LandRandomPos.getPosAway(villager, FLEE_RADIUS, FLEE_Y_RANGE, hostile.position());
      if (fleePos == null)
         // no position on land, so settle for anywhere away from the hostile rather than standing still
         fleePos = DefaultRandomPos.getPosAway(villager, FLEE_RADIUS, FLEE_Y_RANGE, hostile.position());

      if (fleePos != null)
         villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(fleePos, moveSpeed, 0));
   }

   private static boolean canHideInSettlement(CivilizedVillager villager) {
      if (villager.getInfo().getSettlementId() == null)
         return false;

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(villager.getInfo().getSettlementId());
      return settlement.isPresent() && settlement.get()
            .getBounds()
            .getOrigin()
            .closerToCenterThan(villager.position(), MAX_DISTANCE_FROM_SETTLEMENT_TO_HIDE);
   }

   /**
    * Picks a spot inside the nearest settlement building that has a roof. Buildings in unloaded chunks are skipped, to
    * avoid loading chunks while searching them.
    */
   private static Optional<BlockPos> findHidingSpot(ServerLevel level, CivilizedVillager villager) {
      Set<Building> buildings = ServerBuildingsStore.INSTANCE.findForSettlement(villager.getInfo().getSettlementId());

      List<Building> nearestFirst =
            buildings.stream()
                  .filter(b -> b.getDimension().equals(level.dimension()))
                  .filter(
                        b -> level.isLoaded(b.getBounds().getLowerCorner())
                              && level.isLoaded(b.getBounds().getUpperCorner()))
                  .sorted(Comparator.comparingDouble(b -> b.getBlockPos().distToCenterSqr(villager.position())))
                  .toList();

      for (Building building : nearestFirst) {
         List<BlockPos> floorBlocks = building.getBounds().findValidInsideFloorBlocks(level).stream().toList();
         if (!floorBlocks.isEmpty())
            return Optional.of(floorBlocks.get(villager.getRandom().nextInt(floorBlocks.size())));
      }

      return Optional.empty();
   }
}
