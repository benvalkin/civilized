package com.uncreated.civilized.entity.behaviour.worker.soldier;

import java.util.Objects;

import com.uncreated.civilized.core.settlement.defense.TargetRequestResult;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.entity.equipment.VillagerEquipment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;

public class MeleeAttackTarget extends StatefulBehaviour {

   private final float moveSpeed;

   public MeleeAttackTarget(BehaviourState state, float moveSpeed) {
      super(state, Integer.MAX_VALUE, Integer.MAX_VALUE);
      this.moveSpeed = moveSpeed;
   }

   private CarriedEquipmentSlot weaponSlot = CarriedEquipmentSlot.PRIMARY_WEAPON;

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      VillagerEquipment equipment = villager.getEquipment();
      ItemStack equippedOrCarried = equipment.getEquippedOrCarried(weaponSlot);
      if (!equippedOrCarried.has(DataComponents.TOOL)) {
         weaponSlot = CarriedEquipmentSlot.SECONDARY_WEAPON;
         equippedOrCarried = equipment.getEquippedOrCarried(weaponSlot);
      }

      if (!equippedOrCarried.has(DataComponents.TOOL))
         return false;

      if (!villager.requestTargetFromCommand().targetFound())
         return false;

      return true;
   }

   @Override
   protected void lateStart(ServerLevel level, CivilizedVillager villager, long gameTime) {
      Objects.requireNonNull(villager.getTarget());
      villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(villager.getTarget(), moveSpeed, 1));
      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(villager.getTarget(), true));
      nextAttack = 0;
      nextRerequestTarget = 0;
      villager.getEquipment().equip(weaponSlot);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getTarget() != null && villager.getTarget().isAlive();
   }

   long nextAttack;
   long nextRerequestTarget;
   int attackSpeed = 20;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long currentTicks) {

      if (currentTicks > nextRerequestTarget) {
         nextRerequestTarget = currentTicks + 20;

         TargetRequestResult result = villager.requestTargetFromCommand();
         if (result.targetFound()) {
            Objects.requireNonNull(villager.getTarget());
            villager.getBrain()
                  .setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(villager.getTarget(), moveSpeed, 1));
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(villager.getTarget(), true));
         }
      }

      LivingEntity target = villager.getTarget();
      if (target == null)
         return;

      if (!villager.isWithinMeleeAttackRange(target))
         return;

      if (currentTicks > nextAttack) {
         nextAttack = currentTicks + attackSpeed;
         villager.swing(InteractionHand.MAIN_HAND, true);
         villager.addWorkExhaustion(1);
         villager.doHurtTarget(level, target);
      }
   }
}
