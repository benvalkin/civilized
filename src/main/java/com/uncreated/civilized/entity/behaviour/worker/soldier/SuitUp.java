package com.uncreated.civilized.entity.behaviour.worker.soldier;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.entity.equipment.VillagerEquipment;

import net.minecraft.server.level.ServerLevel;

public class SuitUp extends StatefulBehaviour {

   public SuitUp() {
      super(WorkStates.SUIT_UP, 0, 120 * 20);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      return false; // stop immediately (once-off behaviour)
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.CHECK_START, level.getGameTime()))
         return false;

      getBehaviourCooldowns()
            .startCooldown(Cooldowns.CHECK_START, Duration.of(5, ChronoUnit.SECONDS), level.getGameTime());

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime()))
         return false;

      boolean shouldStart = false;
      for (CarriedEquipmentSlot slot : VillagerEquipment.COMBAT_SLOTS) {
         if (!villager.getEquipment().isCarrying(slot))
            continue;

         if (!villager.getEquipment().hasEquipped(slot))
            shouldStart = true;
      }

      return shouldStart;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      villager.getEquipment().equip(VillagerEquipment.COMBAT_SLOTS);

      getBehaviourCooldowns().startCooldown(Cooldowns.START, Duration.of(10, ChronoUnit.SECONDS), level.getGameTime());
   }
}
