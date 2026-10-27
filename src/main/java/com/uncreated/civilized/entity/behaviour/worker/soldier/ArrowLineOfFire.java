package com.uncreated.civilized.entity.behaviour.worker.soldier;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Predicts the flight of an arrow fired by a mob, mirroring {@code AbstractArrow#tick} (move, then drag, then gravity).
 */
public final class ArrowLineOfFire {

   private static final double ARROW_GRAVITY = 0.05;
   private static final double ARROW_DRAG = 0.99;
   /** Vanilla inflates entity hitboxes by this much when checking for arrow hits. */
   private static final double ARROW_HIT_INFLATION = 0.3;
   /** Roughly one standard deviation of a villager's arrow spread. */
   private static final double SPREAD_PER_BLOCK = 0.05;
   private static final int MAX_SIMULATED_TICKS = 60;

   private ArrowLineOfFire() {
   }

   private static final double MIN_PITCH = -Math.PI / 3;
   /** Roughly the furthest-reaching angle, which targets out of range end up with. */
   private static final double MAX_PITCH = Math.PI / 4;
   private static final int SOLVER_ITERATIONS = 20;
   private static final int LEAD_ITERATIONS = 3;

   /**
    * @return the direction to fire in to hit {@code target}, taking into account projectile drop (low arc is preferred)
    *         as well as the target's movement.
    */
   public static Vec3 aimAt(Vec3 arrowOrigin, LivingEntity target, float velocity) {
      Vec3 aimPoint = new Vec3(target.getX(), target.getY(0.5), target.getZ());
      // vertical movement is mostly jumping and falling, so only horizontal movement is led
      Vec3 targetMotion = target.getDeltaMovement().multiply(1, 0, 1);

      ArcSolution arc = solveArc(arrowOrigin, aimPoint, velocity);
      // leading changes the distance and so the flight time, which settles over a few rounds
      for (int i = 0; i < LEAD_ITERATIONS; i++)
         arc = solveArc(arrowOrigin, aimPoint.add(targetMotion.scale(arc.flightTicks())), velocity);

      return arc.direction();
   }

   private record ArcSolution(Vec3 direction, double flightTicks) {
   }

   private static ArcSolution solveArc(Vec3 arrowOrigin, Vec3 aimPoint, float velocity) {
      Vec3 offset = aimPoint.subtract(arrowOrigin);
      double horizontalDistance = offset.horizontalDistance();
      if (horizontalDistance < 0.01)
         return new ArcSolution(offset, offset.length() / velocity);

      // a higher pitch always arrives higher up, so we bisect for the pitch that arrives at the aim point's height
      double lowestPitch = MIN_PITCH;
      double highestPitch = MAX_PITCH;
      for (int i = 0; i < SOLVER_ITERATIONS; i++) {
         double pitch = (lowestPitch + highestPitch) / 2;
         if (arrivalAt(pitch, horizontalDistance, velocity).height() < offset.y)
            lowestPitch = pitch;
         else
            highestPitch = pitch;
      }

      double pitch = (lowestPitch + highestPitch) / 2;
      double horizontalScale = Math.cos(pitch) / horizontalDistance;
      Vec3 direction = new Vec3(offset.x * horizontalScale, Math.sin(pitch), offset.z * horizontalScale);
      return new ArcSolution(direction, arrivalAt(pitch, horizontalDistance, velocity).ticks());
   }

   private record ArrivalPoint(double height, double ticks) {
   }

   /** Simulates an arrow's flight until it has travelled {@code horizontalDistance} along the ground. */
   private static ArrivalPoint arrivalAt(double pitch, double horizontalDistance, float velocity) {
      double distance = 0;
      double height = 0;
      double horizontalSpeed = velocity * Math.cos(pitch);
      double verticalSpeed = velocity * Math.sin(pitch);

      for (int tick = 0; tick < MAX_SIMULATED_TICKS; tick++) {
         if (distance + horizontalSpeed >= horizontalDistance) {
            double fraction = (horizontalDistance - distance) / horizontalSpeed;
            return new ArrivalPoint(height + verticalSpeed * fraction, tick + fraction);
         }

         distance += horizontalSpeed;
         height += verticalSpeed;
         horizontalSpeed *= ARROW_DRAG;
         verticalSpeed = verticalSpeed * ARROW_DRAG - ARROW_GRAVITY;
      }

      // out of range
      return new ArrivalPoint(Double.NEGATIVE_INFINITY, MAX_SIMULATED_TICKS);
   }

   public static Vec3 getArrowOrigin(LivingEntity shooter) {
      return new Vec3(shooter.getX(), shooter.getEyeY() - 0.1F, shooter.getZ());
   }

   /** @return the first entity matching {@code shouldAvoidHitting} on the arrow's predicted path to {@code target}. */
   public static Optional<LivingEntity> findBlockingEntity(
         LivingEntity shooter,
         LivingEntity target,
         float velocity,
         Predicate<LivingEntity> shouldAvoidHitting) {

      Vec3 origin = getArrowOrigin(shooter);
      Vec3 motion = aimAt(origin, target, velocity).normalize().scale(velocity);
      double targetDistance = origin.distanceTo(target.getBoundingBox().getCenter());

      double maxMargin = ARROW_HIT_INFLATION + SPREAD_PER_BLOCK * targetDistance + 1;
      AABB searchArea =
            new AABB(origin, target.getBoundingBox().getCenter()).inflate(maxMargin, maxMargin + 2, maxMargin);
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
         // end the last segment at the target, so entities just behind it don't count
         double stepLength = Math.min(motion.length(), targetDistance - travelled);
         Vec3 next = position.add(motion.normalize().scale(stepLength));
         travelled += stepLength;
         double margin = ARROW_HIT_INFLATION + SPREAD_PER_BLOCK * travelled;

         double closestHitDistance = Double.MAX_VALUE;
         for (LivingEntity candidate : candidates) {
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(margin).clip(position, next);
            // clip finds nothing when the segment starts inside the box
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
