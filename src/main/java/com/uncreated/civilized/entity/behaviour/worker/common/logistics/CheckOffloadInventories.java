package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;

public class CheckOffloadInventories extends WorkTaskBehaviour {


   public CheckOffloadInventories() {
      super(WorkStates.OFFLOAD_INVENTORIES_AFTER_WORK, false,  0, 120 * 20);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      return false; // stop immediately (once-off behaviour)
   }

   private final List<Function<CivilizedVillager, Container>> inventoryTypesToOffload =
         List.of(
               CivilizedVillager::getWorkInputInventory,
               CivilizedVillager::getWorkOutputInventory,
               CivilizedVillager::getLogisticsInventory);

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.CHECK_START, level.getGameTime()))
         return false;

      getBehaviourCooldowns()
            .startCooldown(Cooldowns.CHECK_START, Duration.of(10, ChronoUnit.SECONDS), level.getGameTime());

      for (var inventoryGetter : inventoryTypesToOffload) {
         Container inventory = inventoryGetter.apply(villager);
         if (inventory.hasAnyMatching(i -> !i.isEmpty()))
            return true;
      }

      return false;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {

      DropOffItemsInstruction offloadInventoriesInstruction =
            new DropOffItemsInstruction(getDepot(), inventoryTypesToOffload);

      villager.getBrain().setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), offloadInventoriesInstruction);
      getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);
   }
}
