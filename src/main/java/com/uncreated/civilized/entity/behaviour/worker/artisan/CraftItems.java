package com.uncreated.civilized.entity.behaviour.worker.artisan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.behaviour.ArtisanHouseBehaviour;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.orders.LogisticsOrder;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportOrder;
import com.uncreated.civilized.core.building.production.PendingProductionOutput;
import com.uncreated.civilized.core.building.production.lines.crafting.CraftingMachine;
import com.uncreated.civilized.core.building.production.lines.crafting.CraftingOrder;
import com.uncreated.civilized.core.building.production.orders.ProductionOrder;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

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
   private List<Container> ingredientsChests = new ArrayList<>();
   private List<Container> stockChests = new ArrayList<>();

   public CraftItems() {
      super(WorkStates.CRAFTING_ITEMS, true, false, 90 * 20, 10 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

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

      LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();

      ArtisanHouseBehaviour behaviour = (ArtisanHouseBehaviour) getWorksite().getBehaviour();

      craftingMachine = behaviour.getRecipeProductionSystem().getMachine(CraftingMachine.class);

      ingredientsChests = LogisticsOrder.findChests(level, worksite);
      stockChests =
            storehouse.map(loadedBuilding -> LogisticsOrder.findChests(level, worksite, loadedBuilding.getBuilding()))
                  .orElseGet(() -> ingredientsChests);

      for (CraftingOrder productionOrder : craftingMachine.getOrders()) {
         List<ImportOrder> importOrders = productionOrder.createImportOrdersForIngredients(stockChests);
         importOrders.forEach(i -> logisticsManager.registerOrder(worksite, i));
      }

      if (craftingMachine.tryGetNextOrder(ingredientsChests, stockChests).isEmpty()) {
         // todo: the villager should fetch the missing ingredients from the storehouse. The old import orders were
         // removed with the old logistics system, so this needs a hauling instruction built from the recipe's
         // ingredients before it can work again
         return false;
      }

      return true;
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

      if (itemsCrafted)
         goDropOffWorkOutputAtHome(villager);
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

         Optional<Pair<ProductionOrder, PendingProductionOutput>> nextOrder =
               craftingMachine.tryGetNextOrder(ingredientsChests, stockChests);
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
