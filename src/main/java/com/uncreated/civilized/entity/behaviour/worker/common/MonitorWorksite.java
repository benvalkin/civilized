package com.uncreated.civilized.entity.behaviour.worker.common;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BoundsStroller;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public class MonitorWorksite extends WorkTaskBehaviour {

   /** How often the villager looks around the worksite for dropped items. */
   private static final int PICKUP_INTERVAL_TICKS = 5 * 20;
   private static final int OUTSIDE_TRAVEL_MARGIN = 4;

   private final int maxHorizontalDist;
   private final boolean strollOutside;
   private final BoundsStroller stroller;

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
      super(WorkStates.MONITOR_WORKSITE, true, false, 5 * 20, 0);
      this.maxHorizontalDist = maxHorizontalDist;
      this.strollOutside = strollOutside;
      this.stroller =
            new BoundsStroller(
                  strollOutside ? BoundsStroller.Area.OUTSIDE : BoundsStroller.Area.INSIDE,
                  maxHorizontalDist,
                  maxVerticalDist,
                  strollSpeedModifier);
   }

   public MonitorWorksite(int maxHorizontalDist, int maxVerticalDist, float strollSpeedModifier) {
      this(maxHorizontalDist, maxVerticalDist, strollSpeedModifier, false);
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      stroller.start(gameTime);
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

      stroller.tick(villager, getWorksite().getBuilding().getBounds(), tickTime);
   }
}
