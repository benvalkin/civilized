package com.uncreated.civilized.entity.behaviour.worker.soldier;

import java.util.Objects;

import com.uncreated.civilized.core.settlement.defense.ICombatCommand;
import com.uncreated.civilized.core.settlement.defense.TargetRequestResult;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Ranged combat using a regular bow. Based on the timing of vanilla's {@code RangedBowAttackGoal}, but without the
 * strafing: the villager walks towards its target until it is within range and has had line of sight for a while, then
 * stands still, draws the bow and fires.
 * <p>
 * Shots are held back while something that shouldn't be hit (see {@link ICombatCommand#shouldAvoidHitting}) is in the
 * arrow's path. If the path stays blocked, the villager steps to the side to find a clear shot.
 */
public class BowAttackTarget extends StatefulBehaviour {

   /** Ticks the bow needs to be drawn for a full-power shot (see {@link BowItem#getPowerForTime}). */
   private static final int FULL_DRAW_TICKS = 20;
   /** Ticks of continuous line of sight needed before the villager stops moving to shoot. */
   private static final int SEE_TIME_BEFORE_STOPPING = 20;
   /** Ticks without line of sight after which a drawn bow is lowered again. */
   private static final int LOST_SIGHT_TIMEOUT = 60;
   /** Ticks a fully drawn shot can be blocked before the villager moves to find a clear line of fire. */
   private static final int BLOCKED_TICKS_BEFORE_REPOSITIONING = 30;
   private static final int REPOSITION_DISTANCE = 3;
   /** Maximum ticks spent walking to a new firing position before going back to normal movement. */
   private static final int REPOSITION_TIMEOUT = 60;

   private final float moveSpeed;
   private final float attackRange;
   private final int attackCooldown;

   /**
    * @param attackRange
    *           maximum distance in blocks at which the villager will fire
    * @param attackCooldown
    *           ticks to wait after firing before drawing the bow again
    */
   public BowAttackTarget(BehaviourState state, float moveSpeed, float attackRange, int attackCooldown) {
      super(state, Integer.MAX_VALUE, Integer.MAX_VALUE);
      this.moveSpeed = moveSpeed;
      this.attackRange = attackRange;
      this.attackCooldown = attackCooldown;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      return !villager.findBow().isEmpty() && villager.requestTargetFromCommand().targetFound();
   }

   @Override
   protected void lateStart(ServerLevel level, CivilizedVillager villager, long gameTime) {
      LivingEntity target = Objects.requireNonNull(villager.getTarget());
      villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, moveSpeed, 1));
      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
      nextDraw = 0;
      nextRerequestTarget = 0;
      seeTime = 0;
      blockedTicks = 0;
      repositionUntil = 0;
      villager.setItemInHand(InteractionHand.MAIN_HAND, villager.findBow());
      villager.setAggressive(true); // used by the renderer to raise the bow
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      villager.stopUsingItem();
      villager.setAggressive(false);
      villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getTarget() != null && villager.getTarget().isAlive();
   }

   long nextDraw;
   long nextRerequestTarget;
   /** Positive while the target is visible, negative while it is not, counting ticks since that last changed. */
   int seeTime;
   int blockedTicks;
   long repositionUntil;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long currentTicks) {

      if (currentTicks > nextRerequestTarget) {
         nextRerequestTarget = currentTicks + 20;

         TargetRequestResult result = villager.requestTargetFromCommand();
         if (!result.targetFound())
            return; // canStillUse will stop the behaviour next tick

         if (result == TargetRequestResult.ACQUIRED_NEW_TARGET) {
            seeTime = 0;
            blockedTicks = 0;
            repositionUntil = 0;
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(villager.getTarget(), true));
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         }
      }

      LivingEntity target = villager.getTarget();
      Objects.requireNonNull(target);

      boolean canSee = villager.getSensing().hasLineOfSight(target);
      if (canSee != seeTime > 0)
         seeTime = 0;
      seeTime += canSee ? 1 : -1;

      boolean inRange = villager.closerThan(target, attackRange);

      boolean repositioning =
            currentTicks < repositionUntil && villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);

      if (repositioning) {
         // let the villager finish walking to its new firing position
      } else if (inRange && seeTime >= SEE_TIME_BEFORE_STOPPING) {
         villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      } else if (!villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
         villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, moveSpeed, 1));
      }

      if (villager.isUsingItem()) {
         if (!canSee && seeTime < -LOST_SIGHT_TIMEOUT) {
            villager.stopUsingItem();
            return;
         }

         // keep the bow drawn until the shot is lined up
         int drawTicks = villager.getTicksUsingItem();
         if (canSee && inRange && drawTicks >= FULL_DRAW_TICKS) {
            if (isLineOfFireBlocked(villager, target)) {
               blockedTicks++;
               if (blockedTicks >= BLOCKED_TICKS_BEFORE_REPOSITIONING && !repositioning) {
                  blockedTicks = 0;
                  moveToSide(villager, target, currentTicks);
               }
               return;
            }

            blockedTicks = 0;
            villager.stopUsingItem();
            villager.performRangedAttack(target, BowItem.getPowerForTime(drawTicks));
            nextDraw = currentTicks + attackCooldown;
         }
      } else if (currentTicks >= nextDraw && seeTime >= -LOST_SIGHT_TIMEOUT) {
         villager.startUsingItem(ProjectileUtil.getWeaponHoldingHand(villager, item -> item instanceof BowItem));
      }
   }

   private boolean isLineOfFireBlocked(CivilizedVillager villager, LivingEntity target) {
      ICombatCommand command = villager.getCombatCommand();
      if (command == null)
         return false;

      return ArrowLineOfFire
            .findBlockingEntity(
                  villager,
                  target,
                  CivilizedVillager.ARROW_VELOCITY,
                  entity -> command.shouldAvoidHitting(villager, entity))
            .isPresent();
   }

   /** Walks a few blocks to the left or right of the target, to get a different angle on it. */
   private void moveToSide(CivilizedVillager villager, LivingEntity target, long currentTicks) {
      Vec3 toTarget = target.position().subtract(villager.position());
      Vec3 sideways = new Vec3(-toTarget.z, 0, toTarget.x).normalize();
      if (villager.getRandom().nextBoolean())
         sideways = sideways.reverse();

      Vec3 position =
            LandRandomPos.getPosTowards(
                  villager,
                  REPOSITION_DISTANCE,
                  1,
                  villager.position().add(sideways.scale(REPOSITION_DISTANCE)));
      if (position == null)
         return;

      villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(position, moveSpeed, 0));
      repositionUntil = currentTicks + REPOSITION_TIMEOUT;
   }
}
