package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class DropOffItemsAtBuilding extends WorkTaskBehaviour {
   private MediumDistanceTravelTask travelHelper;
   private DropOffItemsInstruction haulingInstruction;

   public DropOffItemsAtBuilding() {
      super(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING, false, false, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      if (!super.checkExtraStartConditions(level, villager))
         return false;

      Optional<DropOffItemsInstruction> dropOffItemsInstruction =
            villager.getBrain().getMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get());
      if (dropOffItemsInstruction.isEmpty())
         return false;

      haulingInstruction = dropOffItemsInstruction.get();

      // we don't check if the requirement is already satisfied during start attempts.
      // this is only done after picking up.
      return true;
   }

   private Component sampleItemName;

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      ChestBlockEntity chest = haulingInstruction.destinationBuilding().anyChest().orElseThrow();
      travelHelper = new MediumDistanceTravelTask(villager, chest.getBlockPos(), 2);

      findSampleItemForActivityDesc(villager);
   }

   private void findSampleItemForActivityDesc(CivilizedVillager villager) {
      for (VillagerInventoryType inventoryType : haulingInstruction.inventoriesToOffload()) {
         SimpleContainer inventory = villager.getInventory(inventoryType);
         Optional<ItemStack> item = inventory.getItems().stream().filter(i -> !i.isEmpty()).findFirst();
         if (item.isPresent()) {
            sampleItemName = item.get().getItemName();
            break;
         }
      }
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      eraseMemory(villager);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      dropOffItems(level, villager, tickTime);

      doStop(level, villager, tickTime);
   }

   protected void dropOffItems(ServerLevel level, CivilizedVillager villager, long tickTime) {

      List<Container> chests = haulingInstruction.destinationBuilding().chests();

      for (VillagerInventoryType villagerInventoryType : haulingInstruction.inventoriesToOffload()) {
         SimpleContainer inventory = villager.getInventory(villagerInventoryType);
         dumpInventoryToChests(inventory, chests);
      }
   }

   private void eraseMemory(CivilizedVillager villager) {
      villager.getBrain().eraseMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get());
   }

   @Override
   public Component description() {
      if (sampleItemName == null)
         return super.description();

      return Component.translatableWithFallback(
            "villager.behaviour.dropping_off_items_at_building.description.with_args",
            "",
              sampleItemName);
   }
}
