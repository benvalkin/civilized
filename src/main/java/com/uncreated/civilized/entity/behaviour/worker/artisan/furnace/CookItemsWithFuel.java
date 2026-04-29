package com.uncreated.civilized.entity.behaviour.worker.artisan.furnace;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
import com.uncreated.civilized.core.building.production.RecipeProductionSystem;
import com.uncreated.civilized.core.building.production.lines.singleitem.SingleItemRecipeOrder;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.CookingMachine;
import com.uncreated.civilized.core.building.production.orders.ProductionOrder;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class CookItemsWithFuel extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private AbstractFurnaceBlockEntity furnaceBlockEntity;
   private MediumDistanceTravelTask travelHelper;

   private CookingMachine cookingMachine;
   private List<Container> ingredientsChests = new ArrayList<>();
   private List<Container> stockChests = new ArrayList<>();

   public CookItemsWithFuel(BehaviourState state) {
      super(state, true, false, 60 * 20, 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.START, level.getGameTime()))
         return false;

      Optional<LoadedBuilding> storehouse = findStorehouse(getSettlement());

      Building worksite = getWorksite().getBuilding();
      worksite.getBounds().traverseBlocksWithin(traversal -> {
         BlockEntity blockEntity = level.getBlockEntity(traversal.getCurrentBlockPos());

         if (!(blockEntity instanceof AbstractFurnaceBlockEntity furnaceBlockEntity))
            return;

         if (isCorrectFurnaceBlock(furnaceBlockEntity)) {
            this.furnaceBlockEntity = furnaceBlockEntity;
            traversal.terminate();
         }

         if (level.canSeeSky(traversal.getCurrentBlockPos()))
            traversal.skipToNextXZ();
      });

      if (furnaceBlockEntity == null)
         return false;

      LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();

      ArtisanHouseBehaviour behaviour = (ArtisanHouseBehaviour) getHome().getBehaviour();

      cookingMachine = getProductionMachine(behaviour.getRecipeProductionSystem());

      ingredientsChests = LogisticsOrder.findChests(level, worksite);
      stockChests =
            storehouse.map(loadedBuilding -> LogisticsOrder.findChests(level, worksite, loadedBuilding.getBuilding()))
                  .orElseGet(() -> ingredientsChests);

      for (SingleItemRecipeOrder productionOrder : cookingMachine.getOrders()) {
         List<ImportOrder> importOrders = productionOrder.createImportOrdersForIngredients(stockChests);
         importOrders.forEach(i -> logisticsManager.registerOrder(worksite, i));
      }

      if (cookingMachine.tryGetNextOrder(ingredientsChests, stockChests).isEmpty()) {
         // if we cannot craft right now, we should try do a logistics run instead
         getStateMachine().queueActionOnce(WorkStates.FETCHING_IMPORTS_FROM_STOREHOUSE);

         return false;
      }

      return true;
   }

   protected abstract CookingMachine getProductionMachine(RecipeProductionSystem recipeProductionSystem);

   protected abstract boolean isCorrectFurnaceBlock(AbstractFurnaceBlockEntity furnaceBlockEntity);

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, furnaceBlockEntity.getBlockPos(), 2);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getBrain().checkMemory(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(gameTime);
         return;
      }

      villager.getBrain()
            .setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(furnaceBlockEntity.getBlockPos()));

      boolean useSuccess = false;
      boolean changeItemsNeeded = false;

      // take cooked stuff out first

      ItemStack cookedGoods = furnaceBlockEntity.getItem(2).copy();
      if (!cookedGoods.isEmpty() && furnaceBlockEntity.canTakeItem(villager.getWorkOutputInventory(), 3, cookedGoods)) {
         villager.getWorkOutputInventory().addItem(cookedGoods);
         furnaceBlockEntity.setItem(2, ItemStack.EMPTY);

         villager.setItemSlot(EquipmentSlot.MAINHAND, cookedGoods);

         cookingMachine.consumeTokens(cookedGoods.getCount());
         changeItemsNeeded = cookingMachine.getProductionTokens() == 0;

         getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_WORK_OUTPUT_AT_HOME);

         useSuccess = true;
      }

      Optional<Pair<ProductionOrder, PendingProductionOutput>> currentOrder =
            cookingMachine.tryGetNextOrder(ingredientsChests, stockChests);
      if (currentOrder.isEmpty()) {
         // nothing more to cook
         doStop(level, villager, gameTime);

         if (useSuccess)
            villager.swing(InteractionHand.MAIN_HAND, true);

         return;
      }

      PendingProductionOutput pendingOutput = currentOrder.get().getSecond();
      List<PendingProductionOutput.ConsumableIngredientStack> toSmelt = pendingOutput.getConsumableIngredients();

      PendingProductionOutput.ConsumableIngredientStack fuel =
            pendingOutput.getConsumableFuel(cookingMachine.getProductionType().recipeType(), 8, level);

      // try place fuel, or leave alone if there is already fuel
      if (!fuel.subStacks().isEmpty()) {
         ItemStack newInput = fuel.aggregateStack();
         if (furnaceBlockEntity.getItem(1).isEmpty() && furnaceBlockEntity.canPlaceItem(1, newInput)) {
            furnaceBlockEntity.setItem(1, newInput);
            pendingOutput.consumeIngredients(List.of(fuel));
            useSuccess = true;
         }
      }

      // try place cooking ingredients, either by adding to the existing stack, or replacing it
      if (!toSmelt.isEmpty()) {
         ItemStack newInput = toSmelt.getFirst().aggregateStack();
         ItemStack currentlyInFurnace = furnaceBlockEntity.getItem(0).copy();

         if (currentlyInFurnace.isEmpty()) {
            furnaceBlockEntity.setItem(0, newInput);
            pendingOutput.consumeIngredients(toSmelt);
            useSuccess = true;
         } else if (!currentlyInFurnace.is(newInput.getItem()) && changeItemsNeeded) {
            // replace items if order's items are different to what's already in the furnace, adding the old items back
            // to building chests
            if (ContainerHelper.addItemNicely(ingredientsChests, currentlyInFurnace).isEmpty()) {
               furnaceBlockEntity.setItem(0, newInput);
               pendingOutput.consumeIngredients(toSmelt);
               useSuccess = true;
            }
         }
         // leave alone if it's the same item type already in the furnace
      }

      if (useSuccess) {
         villager.swing(InteractionHand.MAIN_HAND, true);
         getBehaviourCooldowns().startCooldown(Cooldowns.START, Duration.of(7, ChronoUnit.SECONDS), gameTime);
      }

      doStop(level, villager, gameTime);
   }
}
