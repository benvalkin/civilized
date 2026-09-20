package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.ConditionalHaulingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TransferToBuildingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TakeItemsToInventory extends WorkTaskBehaviour {
   private MediumDistanceTravelTask travelHelper;

   public TakeItemsToInventory() {
      super(WorkStates.TAKING_ITEMS_TO_INVENTORY, false, false, 120 * 20, 30 * 20);
   }

   private ConditionalHaulingInstruction<? extends ItemStockRequirement> haulingInstruction;
   private int currentSourceBuildingIndex;
   private LoadedBuilding currentSourceBuilding;

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      if (!super.checkExtraStartConditions(level, villager))
         return false;

      Optional<ConditionalHaulingInstruction<? extends ItemStockRequirement>> haulingInstructionMemory =
            villager.getBrain().getMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get());
      if (haulingInstructionMemory.isEmpty())
         return false;

      haulingInstruction = haulingInstructionMemory.get();

      Optional<LoadedBuilding> nextBuilding = findNextBuildingWithStock(villager);
      if (nextBuilding.isEmpty()) {
         // no more buildings to check, all seemed to be empty
         eraseMemory(villager);
         return false;
      }

      acceptNextTargetBuilding(villager, nextBuilding.get());

      // we don't check if the requirement is already satisfied during start attempts.
      // this is only done after picking up.
      return true;
   }

   private Optional<LoadedBuilding> findNextBuildingWithStock(CivilizedVillager villager) {
      for (currentSourceBuildingIndex =
            0; currentSourceBuildingIndex < haulingInstruction.sourceBuildings().size(); currentSourceBuildingIndex++) {

         LoadedBuilding candidateBuilding = haulingInstruction.sourceBuildings().get(currentSourceBuildingIndex);

         // a building is worth visiting when it stocks anything for a requirement the villager still needs items for
         if (haulingInstruction.hasUsefulStock(villager, candidateBuilding))
            return Optional.of(candidateBuilding);
      }
      return Optional.empty();
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      eraseMemory(villager); // TODO: bug: queuing two of this behaviour back will not work properly if we erase the
                             // memory here (the second one's memory will be erased. We need a smarter system that
                             // allows memories to be queued along with behavior states.
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      takeItems(level, villager, tickTime);

      doStop(level, villager, tickTime);
   }

   protected void takeItems(ServerLevel level, CivilizedVillager villager, long tickTime) {

      ConditionalHaulingInstruction.HaulDecision haulDecision =
            haulingInstruction.takeItemsUntilSatisfied(villager, currentSourceBuilding);

      if (haulDecision == ConditionalHaulingInstruction.HaulDecision.REQUIREMENT_SATISFIED_NOTHING_MORE_TO_DO) {
         doStop(level, villager, tickTime);
      } else if (haulDecision == ConditionalHaulingInstruction.HaulDecision.REQUIREMENT_NOT_YET_SATISFIED) {
         Optional<LoadedBuilding> nextBuildingWithStock = findNextBuildingWithStock(villager);
         if (nextBuildingWithStock.isEmpty()) {

            if (haulingInstruction instanceof TransferToBuildingInstruction transferToBuildingInstruction) {
               // try offload whatever we've been able to take
               AggregateItemStack taken = haulingInstruction.countCarriedItems(villager);
               if (taken.hasItems()) {
                  DropOffItemsInstruction dropOffItemsInstruction =
                        new DropOffItemsInstruction(
                              transferToBuildingInstruction.destinationBuilding(),
                              List.of(VillagerInventoryType.LOGISTICS));
                  villager.getBrain()
                        .setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), dropOffItemsInstruction);
                  getStateMachine().queueImmediately(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);
               }
            }

            doStop(level, villager, tickTime);
            return;
         }

         acceptNextTargetBuilding(villager, nextBuildingWithStock.get());
      } else if (haulDecision == ConditionalHaulingInstruction.HaulDecision.REQUIREMENT_SATISFIED_OFFLOAD_ITEMS
            && haulingInstruction instanceof TransferToBuildingInstruction takeToBuildingInstruction) {
         DropOffItemsInstruction dropOffItemsInstruction =
               new DropOffItemsInstruction(
                     takeToBuildingInstruction.destinationBuilding(),
                     List.of(VillagerInventoryType.LOGISTICS));
         villager.getBrain().setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), dropOffItemsInstruction);
         getStateMachine().queueImmediately(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);
         doStop(level, villager, tickTime);
      } else
         throw new UnsupportedOperationException(
               "Unsupported hauling decision for hauling instruction '" + haulingInstruction.getClass() + "': "
                     + haulDecision);
   }

   private void acceptNextTargetBuilding(CivilizedVillager villager, LoadedBuilding nextBuildingWithStock) {
      currentSourceBuilding = nextBuildingWithStock;
      BlockEntity chest = currentSourceBuilding.findAnyChest().orElseThrow();
      travelHelper = new MediumDistanceTravelTask(villager, chest.getBlockPos(), 2);
   }

   private void eraseMemory(CivilizedVillager villager) {
      villager.getBrain().eraseMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get());
   }
}
