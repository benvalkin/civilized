package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;

/**
 * Checks if the villager should go dump all their inventories at home. Intended to trigger once when the villager gets
 * off work so that they don't keep items in their inventory.
 */
public class CheckOffloadInventoriesAtHome extends WorkTaskBehaviour {

   public CheckOffloadInventoriesAtHome() {
      super(BehaviourStates.OFFLOAD_INVENTORIES_AFTER_WORK, false, true, 0, 120 * 20);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      return false; // stop immediately (once-off behaviour)
   }

   private final List<VillagerInventoryType> inventoryTypesToOffload =
         List.of(VillagerInventoryType.WORK_TASK, VillagerInventoryType.WORK_OUTPUT, VillagerInventoryType.LOGISTICS);

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.CHECK_START, level.getGameTime()))
         return false;

      getBehaviourCooldowns()
            .startCooldown(Cooldowns.CHECK_START, Duration.of(10, ChronoUnit.SECONDS), level.getGameTime());

      for (VillagerInventoryType value : inventoryTypesToOffload) {
         SimpleContainer inventory = villager.getInventory(value);
         if (inventory.hasAnyMatching(i -> !i.isEmpty()))
            return true;
      }

      return false;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {

      DropOffItemsInstruction offloadInventoriesInstruction =
            new DropOffItemsInstruction(getHome(), inventoryTypesToOffload);

      villager.getBrain().setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), offloadInventoriesInstruction);
      getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);
   }
}
