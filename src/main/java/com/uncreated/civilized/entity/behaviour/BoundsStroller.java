package com.uncreated.civilized.entity.behaviour;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.entity.CivilizedVillager;

import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BoundsStroller {

   public enum Area {
      /** Anywhere near the building, heading back towards it if the villager has wandered off. */
      INSIDE_OR_NEAR,
      /** Only inside the building's bounds. */
      INSIDE,
      /** Close to the building, but never inside it, e.g. to stay off a farm's crops. */
      OUTSIDE
   }

   private static final int MIN_STROLL_INTERVAL_TICKS = 5 * 20;
   private static final int MAX_STROLL_INTERVAL_TICKS = 15 * 20;
   /** How many random spots are tried before giving up. */
   private static final int ATTEMPTS = 20;

   private final Area area;
   private final int maxHorizontalDist;
   private final int maxVerticalDist;
   private final float speedModifier;
   @Setter
   @Nullable
   private BiConsumer<CivilizedVillager, BuildingBounds> onArrived;
   private boolean arrived;

   private long nextStrollTime;

   /**
    * @param maxHorizontalDist
    *           how far the villager walks during each stroll
    */
   public BoundsStroller(Area area, int maxHorizontalDist, int maxVerticalDist, float speedModifier) {
      this.area = area;
      this.maxHorizontalDist = maxHorizontalDist;
      this.maxVerticalDist = maxVerticalDist;
      this.speedModifier = speedModifier;
   }

   public void start(long gameTime) {
      nextStrollTime = gameTime;
      arrived = false;
   }

   public void tick(CivilizedVillager villager, BuildingBounds bounds, long gameTime) {
      if (gameTime < nextStrollTime)
         return;

      nextStrollTime = gameTime + villager.getRandom().nextInt(MIN_STROLL_INTERVAL_TICKS, MAX_STROLL_INTERVAL_TICKS);

      Vec3 wanderPos = switch (area) {
      case INSIDE_OR_NEAR -> findPosAround(villager, bounds);
      case INSIDE -> findPosWithin(villager, bounds);
      case OUTSIDE -> findPosOutside(villager, bounds);
      };

      if (wanderPos != null)
         villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(wanderPos, speedModifier, 2));
      else
         villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

      boolean arrived = switch (area) {
      case INSIDE_OR_NEAR, INSIDE -> bounds.getEncapsulatingAABB().contains(villager.position());
      case OUTSIDE -> bounds.getEncapsulatingAABB().inflate(1 + maxHorizontalDist).contains(villager.position());
      };

      if (onArrived != null && arrived && !this.arrived) {
         this.arrived = true;
         onArrived.accept(villager, bounds);
      }
   }

   private @Nullable Vec3 findPosAround(CivilizedVillager villager, BuildingBounds bounds) {
      if (bounds.contains(villager.blockPosition()))
         return LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);

      return walkTowards(villager, bounds);
   }

   private @Nullable Vec3 findPosWithin(CivilizedVillager villager, BuildingBounds bounds) {
      if (!bounds.contains(villager.blockPosition()))
         return walkTowards(villager, bounds);

      for (int i = 0; i < ATTEMPTS; i++) {
         Vec3 wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
         if (wanderPos != null && bounds.contains(BlockPos.containing(wanderPos)))
            return wanderPos;
      }

      // rather stay put than wander out of the building
      return null;
   }

   private @Nullable Vec3 findPosOutside(CivilizedVillager villager, BuildingBounds bounds) {
      AABB innerBounds = bounds.getEncapsulatingAABB().inflate(1);
      AABB outerBounds = bounds.getEncapsulatingAABB().inflate(1 + maxHorizontalDist);

      Vec3 wanderPos = null;
      for (int i = 0; i < ATTEMPTS; i++) {
         wanderPos = LandRandomPos.getPos(villager, maxHorizontalDist, maxVerticalDist);
         if (wanderPos == null)
            continue;

         if (outerBounds.contains(wanderPos) && !innerBounds.contains(wanderPos))
            break;
      }

      // rather stay put than wander into the building
      if (wanderPos != null && innerBounds.contains(wanderPos))
         return null;

      return wanderPos;
   }

   private @Nullable Vec3 walkTowards(CivilizedVillager villager, BuildingBounds bounds) {
      return LandRandomPos
            .getPosTowards(villager, maxHorizontalDist, maxVerticalDist, bounds.getCenter().getBottomCenter());
   }
}
