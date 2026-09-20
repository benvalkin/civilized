package com.uncreated.civilized.entity.behaviour.worker.common;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MonitorWorksite extends WorkTaskBehaviour {

   /** How often the villager looks around the worksite for dropped items. */
   private static final int PICKUP_INTERVAL_TICKS = 5 * 20;
   private static final int OUTSIDE_TRAVEL_MARGIN = 4;

   private final int maxHorizontalDist;
   private final int maxVerticalDist;
   private final float speedModifier;
   private final boolean strollOutside;

   private long nextWorkTime;
   private long nextPickupTime;
   private MediumDistanceTravelTask travelHelper;

   /**
    * @param strollOutside
    *           whether the villager should keep out of the worksite's bounds while monitoring it
    */
   public MonitorWorksite(
         int maxHorizontalDist,
         int maxVerticalDist,
         float strollSpeedModifier,
         boolean strollOutside) {
      super(WorkStates.MONITOR_WORKSITE, true, false, 120 * 15, 0);
      this.maxHorizontalDist = maxHorizontalDist;
      this.maxVerticalDist = maxVerticalDist;
      this.speedModifier = strollSpeedModifier;
      this.strollOutside = strollOutside;
   }

   public MonitorWorksite(int maxHorizontalDist, int maxVerticalDist, float strollSpeedModifier) {
      this(maxHorizontalDist, maxVerticalDist, strollSpeedModifier, false);
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      nextWorkTime = gameTime;
      nextPickupTime = gameTime;
      travelHelper = strollOutside ? createTravelToOutsideWorksite(villager) : createTravelToWorksite(villager);
   }

   private MediumDistanceTravelTask createTravelToWorksite(CivilizedVillager villager) {
      return new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), maxHorizontalDist + 1);
   }

   private MediumDistanceTravelTask createTravelToOutsideWorksite(CivilizedVillager villager) {
      AABB closeEnoughBounds =
            getWorksite().getBuilding().getBounds().getEncapsulatingAABB().inflate(OUTSIDE_TRAVEL_MARGIN);

      return new MediumDistanceTravelTask(
            villager,
            getWorksite().getBuilding().getBlockPos(),
            (v, d, closeEnough) -> closeEnoughBounds.contains(v.position()),
            Math.max((int) closeEnoughBounds.getXsize() / 2, (int) closeEnoughBounds.getZsize() / 2));
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      if (tickTime >= nextPickupTime) {
         nextPickupTime = tickTime + PICKUP_INTERVAL_TICKS;

         if (pickUpDroppedItemsAtWorksite(level, villager))
            goDropOffWorkOutputAtHome(villager);
      }

      if (tickTime < nextWorkTime)
         return;

      nextWorkTime += villager.getRandom().nextInt(5 * 20, 15 * 20);

      Vec3 wanderPos = strollOutside ? findPosOutsideWorksite(villager) : findPosAroundWorksite(villager);

      if (wanderPos != null)
         villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(wanderPos, speedModifier, 2));
      else
         villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }

   private Vec3 findPosAroundWorksite(CivilizedVillager villager) {
      if (getWorksite().getBuilding().getBounds().contains(villager.blockPosition()))
         return LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);

      return LandRandomPos.getPosTowards(
            villager,
            maxHorizontalDist,
            maxVerticalDist,
            getWorksite().getBuilding().getBlockPos().getBottomCenter());
   }

   private Vec3 findPosOutsideWorksite(CivilizedVillager villager) {
      BuildingBounds worksiteBounds = getWorksite().getBuilding().getBounds();
      AABB innerBounds = worksiteBounds.getEncapsulatingAABB().inflate(1);
      AABB outerBounds = worksiteBounds.getEncapsulatingAABB().inflate(1 + maxHorizontalDist);

      Vec3 wanderPos = null;
      for (int i = 0; i < 20; i++) {
         wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
         if (wanderPos == null)
            continue;

         if (outerBounds.contains(wanderPos) && !innerBounds.contains(wanderPos))
            break;
      }

      // rather stay put than wander into the worksite
      if (wanderPos != null && innerBounds.contains(wanderPos))
         return null;

      return wanderPos;
   }
}
