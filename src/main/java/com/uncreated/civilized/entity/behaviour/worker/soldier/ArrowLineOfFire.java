package com.uncreated.civilized.entity.behaviour.worker.soldier;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Predicts the flight path of an arrow fired by a mob, to check whether anything is standing in the way before the
 * shot is taken. The simulation mirrors the physics in {@code AbstractArrow#tick} (move, then drag, then gravity) and
 * the aiming used by {@code CivilizedVillager#performRangedAttack}.
 */
public final class ArrowLineOfFire {

   private static final double ARROW_GRAVITY = 0.05;
   private static final double ARROW_DRAG = 0.99;
   /** Arrows hit entities whose bounding box, inflated by this amount, intersects the arrow's path. */
   private static final double ARROW_HIT_INFLATION = 0.3;
   /**
    * How far an arrow may stray sideways per block travelled, due to inaccuracy. Roughly one standard deviation of the
    * spread at the villager's inaccuracy, so most (but not all) stray arrows are accounted for.
    */
   private static final double SPREAD_PER_BLOCK = 0.05;
   private static final int MAX_SIMULATED_TICKS = 60;

   private ArrowLineOfFire() {
   }

   /**
    * The direction a mob aims in to hit {@code target} with an arrow fired from {@code arrowOrigin}. Like skeletons,
    * this aims a little above the target to compensate for gravity.
    */
   public static Vec3 aimAt(Vec3 arrowOrigin, LivingEntity target) {
      double dx = target.getX() - arrowOrigin.x;
      double dy = target.getY(1.0 / 3.0) - arrowOrigin.y;
      double dz = target.getZ() - arrowOrigin.z;
      double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
      return new Vec3(dx, dy + horizontalDistance * 0.2F, dz);
   }

   /** Where an arrow fired by {@code shooter} is spawned, matching {@code AbstractArrow}'s constructor. */
   public static Vec3 arrowOrigin(LivingEntity shooter) {
      return new Vec3(shooter.getX(), shooter.getEyeY() - 0.1F, shooter.getZ());
   }

   /**
    * Finds the first entity along the arrow's predicted path (before it reaches {@code target}) that matches
    * {@code shouldAvoidHitting}.
    */
   public static Optional<LivingEntity> findBlockingEntity(
         LivingEntity shooter,
         LivingEntity target,
         float velocity,
         Predicate<LivingEntity> shouldAvoidHitting) {

      Vec3 origin = arrowOrigin(shooter);
      Vec3 motion = aimAt(origin, target).normalize().scale(velocity);
      double targetDistance = origin.distanceTo(target.getBoundingBox().getCenter());

      // gather candidates once, from a box covering the whole straight-line path plus the widest spread margin
      double maxMargin = ARROW_HIT_INFLATION + SPREAD_PER_BLOCK * targetDistance + 1;
      AABB searchArea = new AABB(origin, target.getBoundingBox().getCenter()).inflate(maxMargin, maxMargin + 2, maxMargin);
      List<LivingEntity> candidates =
            shooter.level()
                  .getEntitiesOfClass(
                        LivingEntity.class,
                        searchArea,
                        e -> e != shooter && e != target && e.isAlive() && e.canBeHitByProjectile()
                              && shouldAvoidHitting.test(e));

      if (candidates.isEmpty())
         return Optional.empty();

      Vec3 position = origin;
      double travelled = 0;
      LivingEntity closestHit = null;

      for (int tick = 0; tick < MAX_SIMULATED_TICKS && travelled < targetDistance; tick++) {
         // stop the last segment at the target, so that entities just behind it don't count as blocking
         double stepLength = Math.min(motion.length(), targetDistance - travelled);
         Vec3 next = position.add(motion.normalize().scale(stepLength));
         travelled += stepLength;
         double margin = ARROW_HIT_INFLATION + SPREAD_PER_BLOCK * travelled;

         double closestHitDistance = Double.MAX_VALUE;
         for (LivingEntity candidate : candidates) {
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(margin).clip(position, next);
            // the segment may start inside the box, in which case clip finds nothing
            if (hit.isEmpty() && candidate.getBoundingBox().inflate(margin).contains(position))
               hit = Optional.of(position);

            if (hit.isPresent() && hit.get().distanceToSqr(position) < closestHitDistance) {
               closestHitDistance = hit.get().distanceToSqr(position);
               closestHit = candidate;
            }
         }

         if (closestHit != null)
            return Optional.of(closestHit);

         position = next;
         motion = motion.scale(ARROW_DRAG).subtract(0, ARROW_GRAVITY, 0);
      }

      return Optional.empty();
   }
}
