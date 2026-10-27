package com.uncreated.civilized.entity.behaviour.worker.soldier;

import java.util.Objects;

import com.uncreated.civilized.core.settlement.defense.ICombatCommand;
import com.uncreated.civilized.core.settlement.defense.TargetRequestResult;
import com.uncreated.civilized.core.villagerinfo.skills.SkillEffects;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.entity.equipment.VillagerEquipment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class BowAttackTarget extends StatefulBehaviour {

   /** See {@link BowItem#getPowerForTime}. */
   private static final int FULL_DRAW_TICKS = 20;
   private int drawTicksNeeded = FULL_DRAW_TICKS;
   private static final int SEE_TIME_BEFORE_STOPPING = 10;
   private static final int LOST_SIGHT_TIMEOUT = 60;
   private static final int BLOCKED_TICKS_BEFORE_REPOSITIONING = 30;
   private static final int REPOSITION_DISTANCE = 3;
   private static final int REPOSITION_TIMEOUT = 60;
   private static final double RETREAT_LEAD_DISTANCE = 8;
   private static final int RETREAT_Y_RANGE = 7;

   private final float moveSpeed;
   private final float attackRange;
   private final float minimumDistanceToRetreat;
   private final float distanceToRetreatTo;
   private final int attackCooldown;

   public BowAttackTarget(
         BehaviourState state,
         float moveSpeed,
         float attackRange,
         float minimumDistanceToRetreat,
         float distanceToRetreatTo,
         int attackCooldown) {
      super(state, Integer.MAX_VALUE, Integer.MAX_VALUE);
      this.moveSpeed = moveSpeed;
      this.attackRange = attackRange;
      this.minimumDistanceToRetreat = minimumDistanceToRetreat;
      this.distanceToRetreatTo = distanceToRetreatTo;
      this.attackCooldown = attackCooldown;
   }

   private CarriedEquipmentSlot weaponSlot;

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      VillagerEquipment equipment = villager.getEquipment();

      weaponSlot = CarriedEquipmentSlot.PRIMARY_WEAPON;
      ItemStack equippedOrCarried = equipment.getEquippedOrCarried(weaponSlot);
      if (!(equippedOrCarried.getItem() instanceof BowItem)) {
         weaponSlot = CarriedEquipmentSlot.SECONDARY_WEAPON;
         equippedOrCarried = equipment.getEquippedOrCarried(weaponSlot);
      }
      if (!(equippedOrCarried.getItem() instanceof BowItem)) {
         weaponSlot = CarriedEquipmentSlot.OFFHAND_WEAPON;
         equippedOrCarried = equipment.getEquippedOrCarried(weaponSlot);
      }

      if (!(equippedOrCarried.getItem() instanceof BowItem))
         return false;

      TargetRequestResult targetRequestResult = villager.requestTargetFromCommand();
      if (!targetRequestResult.targetFound())
         return false;

      if (villager.position().distanceToSqr(Objects.requireNonNull(villager.getTarget()).position()) < 6 * 6)
         return false;

      return true;
   }

   @Override
   protected void lateStart(ServerLevel level, CivilizedVillager villager, long gameTime) {
      LivingEntity target = Objects.requireNonNull(villager.getTarget());
      villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, moveSpeed, 1));
      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
      nextDraw = 0;
      drawTicksNeeded = SkillEffects.getBowDrawTicks(villager.getInfo().getSkills().getLevel(VillagerSkill.SHOOTING));
      nextRerequestTarget = 0;
      seeTime = 0;
      retreating = false;
      blockedTicks = 0;
      repositionUntil = 0;
      villager.getEquipment().equip(weaponSlot);
      villager.setAggressive(true); // used by the renderer to raise the bow
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      villager.stopUsingItem();
      villager.setAggressive(false);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getTarget() != null && villager.getTarget().isAlive();
   }

   long nextDraw;
   long nextRerequestTarget;
   /** Ticks the target has been visible (positive) or out of sight (negative). */
   int seeTime;
   int blockedTicks;
   long repositionUntil;
   boolean retreating;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long currentTicks) {

      if (currentTicks > nextRerequestTarget) {
         nextRerequestTarget = currentTicks + 20;

         TargetRequestResult result = villager.requestTargetFromCommand();
         if (!result.targetFound())
            return; // canStillUse will stop the behaviour next tick

         if (result == TargetRequestResult.ACQUIRED_NEW_TARGET) {
            seeTime = 0;
            retreating = false;
            blockedTicks = 0;
            repositionUntil = 0;
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(villager.getTarget(), true));
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         }
      }

      LivingEntity target = villager.getTarget();
      Objects.requireNonNull(target);

      // LookAtTargetSink erases the look target whenever sight is lost
      if (!villager.getBrain().hasMemoryValue(MemoryModuleType.LOOK_TARGET))
         villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));

      boolean canSee = villager.getSensing().hasLineOfSight(target);
      if (canSee != seeTime > 0)
         seeTime = 0;
      seeTime += canSee ? 1 : -1;

      boolean inRange = villager.closerThan(target, attackRange);

      boolean repositioning =
            currentTicks < repositionUntil && villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);

      // keep retreating until well clear, not just out of the distance that started it
      if (retreating)
         retreating = villager.closerThan(target, distanceToRetreatTo);
      else
         retreating = villager.closerThan(target, minimumDistanceToRetreat);

      if (repositioning) {
         // finish walking to the new firing position
      } else if (retreating) {
         retreatFrom(villager, target);
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

         int drawTicks = villager.getTicksUsingItem();
         if (canSee && inRange && drawTicks >= drawTicksNeeded) {
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
            // skill only changes the draw time, so every shot is at full power
            villager.performRangedAttack(target, BowItem.getPowerForTime(FULL_DRAW_TICKS));
            nextDraw = currentTicks + attackCooldown;
         }
      } else if (currentTicks >= nextDraw && seeTime >= -LOST_SIGHT_TIMEOUT) {
         villager.startUsingItem(ProjectileUtil.getWeaponHoldingHand(villager, item -> item instanceof BowItem));
      }
   }

   /** The walk target is moved ahead every tick, so the villager keeps running instead of stopping at each spot. */
   private void retreatFrom(CivilizedVillager villager, LivingEntity target) {

      if (villager.getBrain().hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
         // backing straight away is blocked, e.g. by a wall
         Vec3 randomPos =
               LandRandomPos.getPosAway(villager, (int) RETREAT_LEAD_DISTANCE, RETREAT_Y_RANGE, target.position());
         if (randomPos == null)
            return;

         villager.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(randomPos, moveSpeed * 1.25f, 1));
         return;
      }

      Vec3 awayFromTarget = villager.position().subtract(target.position());
      if (awayFromTarget.horizontalDistanceSqr() < 1.0E-4)
         return; // standing on top of each other, so there is no direction to back away in

      Vec3 retreatPos = villager.position().add(awayFromTarget.normalize().scale(RETREAT_LEAD_DISTANCE));
      villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(retreatPos, moveSpeed * 1.25f, 1));
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
