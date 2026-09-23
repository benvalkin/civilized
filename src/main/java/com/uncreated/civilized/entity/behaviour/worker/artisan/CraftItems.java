package com.uncreated.civilized.entity.behaviour.worker.artisan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.behaviour.ArtisanHouseBehaviour;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TransferToBuildingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.BuildingStockRequirement;
import com.uncreated.civilized.core.building.production.PendingProductionOutput;
import com.uncreated.civilized.core.building.production.lines.crafting.CraftingMachine;
import com.uncreated.civilized.core.building.production.lines.crafting.CraftingOrder;
import com.uncreated.civilized.core.building.production.orders.ProductionOrder;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CraftItems extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private BlockPos workBlock;
   private MediumDistanceTravelTask travelHelper;

   int workSpeedMultiplier = 2;
   private CraftingMachine craftingMachine;

   public CraftItems() {
      super(WorkStates.CRAFTING_ITEMS, true, false, 90 * 20, 10 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      Optional<LoadedBuilding> storehouse = findStorehouse(getSettlement());

      Building worksite = getWorksite().getBuilding();
      worksite.getBounds().traverseBlocksWithin(traversal -> {
         BlockState blockState = level.getBlockState(traversal.getCurrentBlockPos());

         if (blockState.getBlock() instanceof CraftingTableBlock) {
            workBlock = traversal.getCurrentBlockPos();
            traversal.terminate();
         }

         if (level.canSeeSky(traversal.getCurrentBlockPos()))
            traversal.skipToNextXZ();
      });

      if (workBlock == null)
         return false;

      ArtisanHouseBehaviour behaviour = (ArtisanHouseBehaviour) getWorksite().getBehaviour();

      craftingMachine = behaviour.getRecipeProductionSystem().getMachine(CraftingMachine.class);

      List<Container> worksiteChests = getWorksite().chests();
      List<Container> storehouseChests = storehouse.map(LoadedBuilding::chests).orElse(List.of());
      List<Container> storehouseAndWorksiteChests =
            Stream.concat(worksiteChests.stream(), storehouseChests.stream()).toList();

      if (craftingMachine.tryGetNextOrder(worksiteChests, storehouseAndWorksiteChests).isEmpty()) {

         // we cannot produce anything at the moment, so we should try import ingredients from the storehouse
         if (storehouse.isEmpty())
            return false; // cannot import anything if the storehouse doesn't exist

         List<BuildingStockRequirement> allRecipeStockRequirements =
               createIngredientsRequirementsForAllRecipes(storehouseAndWorksiteChests);

         String party = reservationPartyKey(villager);

         Optional<TransferToBuildingInstruction> fetchFromStorehouse =
               TransferToBuildingInstruction.createIfAnyMetFromSourceBuildings(
                     new ReservationKey(party, "crafting_ingredients"),
                     allRecipeStockRequirements,
                     getWorksite(),
                     List.of(storehouse.get()));
         if (fetchFromStorehouse.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), fetchFromStorehouse.get());
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            getStateMachine().queueActionOnce(this.getState());
         }

         return false;
      }

      return true;
   }

   private @NotNull List<BuildingStockRequirement> createIngredientsRequirementsForAllRecipes(
         List<Container> storehouseAndWorksiteChests) {
      List<BuildingStockRequirement> allRecipeStockRequirements = new ArrayList<>();
      for (CraftingOrder productionOrder : craftingMachine.getOrders()) {

         int finalProductDeficit = productionOrder.calculateFinalProductDeficit(storehouseAndWorksiteChests);
         if (finalProductDeficit <= 0)
            // there is no reason to transport ingredients for bills that are already satisfied
            continue;

         List<BuildingStockRequirement> craftingIngredientsRequirements =
               productionOrder.createStandardIngredientRequirements(0, finalProductDeficit);

         allRecipeStockRequirements.addAll(craftingIngredientsRequirements);
      }
      return allRecipeStockRequirements;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, workBlock, 2);
      itemsCrafted = false;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      // finishing crafting items does not immediately tell the villager go drop off items in the storehouse
      dumpInventoryToChests(villager.getWorkOutputInventory(), getWorksite().chests());
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getBrain().checkMemory(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT);
   }

   private int applyWorkSpeedMultiplier(int workIntervalTicks) {
      return workIntervalTicks / workSpeedMultiplier;
   }

   private boolean itemsCrafted;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(gameTime);
         return;
      }

      if (gameTime - lastWorkTime > applyWorkSpeedMultiplier(30)) {

         lastWorkTime = gameTime;

         List<Container> worksiteChests = getWorksite().chests();
         List<Container> storehouseChests =
               findStorehouse(getSettlement()).map(LoadedBuilding::chests).orElse(List.of());
         List<Container> storehouseAndWorksiteChests =
               Stream.concat(worksiteChests.stream(), storehouseChests.stream()).toList();

         Optional<Pair<ProductionOrder, PendingProductionOutput>> nextOrder =
               craftingMachine.tryGetNextOrder(worksiteChests, storehouseAndWorksiteChests);
         if (nextOrder.isEmpty()) {
            // nothing more to craft
            doStop(level, villager, gameTime);
            return;
         }

         PendingProductionOutput pendingOutput = nextOrder.get().getSecond();
         ItemStack resultItem = pendingOutput.assembledRecipe().resultItem();
         if (!villager.getWorkOutputInventory().canAddItem(resultItem)) {
            // cannot craft recipe because villager's inventory is full
            doStop(level, villager, gameTime);
            return;
         }

         villager.getWorkOutputInventory().addItem(resultItem);
         pendingOutput.consumeIngredients(pendingOutput.getConsumableIngredients());

         craftingMachine.consumeToken();

         villager.swing(InteractionHand.MAIN_HAND, true);
         villager.setItemSlot(EquipmentSlot.MAINHAND, resultItem.copyWithCount(1));
         villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workBlock));

         itemsCrafted = true;
      }
   }
}
