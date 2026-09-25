package com.uncreated.civilized.entity.behaviour.worker.artisan.furnace;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.behaviour.ArtisanHouseBehaviour;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TransferToBuildingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.BuildingStockRequirement;
import com.uncreated.civilized.core.building.production.PendingProductionOutput;
import com.uncreated.civilized.core.building.production.RecipeProductionSystem;
import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.RecipeSlotType;
import com.uncreated.civilized.core.building.production.lines.singleitem.SingleItemRecipeOrder;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.CookingMachine;
import com.uncreated.civilized.core.building.production.orders.ProductionOrder;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class CookItemsWithFuel extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private AbstractFurnaceBlockEntity furnaceBlockEntity;
   private MediumDistanceTravelTask travelHelper;

   private CookingMachine cookingMachine;

   public CookItemsWithFuel(BehaviourState state) {
      super(state, true, false, 60 * 20, 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

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

      ArtisanHouseBehaviour behaviour = (ArtisanHouseBehaviour) getWorksite().getBehaviour();

      cookingMachine = getProductionMachine(behaviour.getRecipeProductionSystem());

      List<Container> worksiteChests = getWorksite().chests();
      List<Container> storehouseChests = storehouse.map(LoadedBuilding::chests).orElse(List.of());
      List<Container> storehouseAndWorksiteChests =
            Stream.concat(worksiteChests.stream(), storehouseChests.stream()).toList();

      if (cookingMachine.tryGetNextOrder(worksiteChests, storehouseAndWorksiteChests).isEmpty()) {

         // there is nothing to put into the furnace
         // we can still take cooked items out though
         ItemStack cookedGoods = furnaceBlockEntity.getItem(2).copy();
         if (!cookedGoods.isEmpty()
               && furnaceBlockEntity.canTakeItem(villager.getWorkOutputInventory(), 3, cookedGoods)) {
            villager.getWorkOutputInventory().addItem(cookedGoods);
            furnaceBlockEntity.setItem(2, ItemStack.EMPTY);

            cookingMachine.consumeTokens(cookedGoods.getCount());

            dumpInventoryToChests(villager.getWorkOutputInventory(), getWorksite().chests());

            villager.swing(InteractionHand.MAIN_HAND, true);
            villager.addWorkExhaustion(1);
         }

         // we cannot produce anything at the moment, so we should try import ingredients from the storehouse
         if (storehouse.isEmpty())
            return false; // cannot import anything if the storehouse doesn't exist

         List<BuildingStockRequirement> allRecipeStockRequirements =
               createIngredientsRequirementsForAllRecipes(storehouseAndWorksiteChests, level);

         String party = reservationPartyKey(villager);

         Optional<TransferToBuildingInstruction> fetchFromStorehouse =
               TransferToBuildingInstruction.createIfAnyMetFromSourceBuildings(
                     new ReservationKey(party, "cooking_ingredients"),
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
         List<Container> storehouseAndWorksiteChests,
         ServerLevel level) {
      List<BuildingStockRequirement> allRecipeStockRequirements = new ArrayList<>();
      for (SingleItemRecipeOrder productionOrder : cookingMachine.getOrders()) {

         int finalProductDeficit = productionOrder.calculateFinalProductDeficit(storehouseAndWorksiteChests);
         if (finalProductDeficit <= 0)
            // there is no reason to transport ingredients for bills that are already satisfied
            continue;

         List<BuildingStockRequirement> cookingIngredientsRequirements =
               productionOrder.createStandardIngredientRequirements(0, finalProductDeficit);

         // A better solution would be to let the RecipeType influence the batch size
         BuildingStockRequirement createFuelRequirement =
               createFuelRequirement(1, productionOrder, finalProductDeficit, level);

         allRecipeStockRequirements.add(createFuelRequirement);

         allRecipeStockRequirements.addAll(cookingIngredientsRequirements);
      }
      return allRecipeStockRequirements;
   }

   private BuildingStockRequirement createFuelRequirement(
         int recipeSlot,
         SingleItemRecipeOrder productionOrder,
         int amount,
         ServerLevel level) {

      RecipeType<?> recipeType = productionOrder.getBill().getProductionType().recipeType();
      RecipeSlotType fuelSlot = productionOrder.getBill().getProductionType().recipeSlots().get(recipeSlot);

      ItemFilter itemFilter = productionOrder.getBill().getItemFilters().getFilter(recipeSlot);
      Comparator<ItemStack> preference = Comparator.comparingInt(i -> -i.getBurnTime(recipeType, level.fuelValues()));

      BuildingStockRequirement requirement =
            new BuildingStockRequirement(
                  String.format(
                        "%s:%s:fuel",
                        productionOrder.getBill().getProductionType(),
                        productionOrder.getBill().getMinecraftRecipeName()),
                  i -> fuelSlot.isItemAllowed(i, level) && itemFilter.acceptsItem(i),
                  preference,
                  1,
                  amount);
      // makes it so that duplicate ingredients are still taken
      requirement.disregardExistingCarriedStock(true);

      return requirement;
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

         dumpInventoryToChests(villager.getWorkOutputInventory(), getWorksite().chests());

         useSuccess = true;
      }

      Optional<LoadedBuilding> storehouse = findStorehouse(getSettlement());
      if (storehouse.isEmpty()) {
         doStop(level, villager, gameTime);
         return;
      }

      List<Container> worksiteChests = getWorksite().chests();
      List<Container> storehouseChests = storehouse.map(LoadedBuilding::chests).orElse(List.of());
      List<Container> storehouseAndWorksiteChests =
            Stream.concat(worksiteChests.stream(), storehouseChests.stream()).toList();

      Optional<Pair<ProductionOrder, PendingProductionOutput>> currentOrder =
            cookingMachine.tryGetNextOrder(worksiteChests, storehouseAndWorksiteChests);
      if (currentOrder.isEmpty()) {
         // nothing more to cook
         doStop(level, villager, gameTime);

         if (useSuccess) {
            villager.swing(InteractionHand.MAIN_HAND, true);
            villager.addWorkExhaustion(1);
         }

         return;
      }

      ProductionOrder order = currentOrder.get().getFirst();
      PendingProductionOutput pendingOutput = currentOrder.get().getSecond();
      List<PendingProductionOutput.ConsumableIngredientStack> toSmelt = pendingOutput.getConsumableIngredients();

      PendingProductionOutput.ConsumableIngredientStack fuel = pendingOutput.getConsumableFuel(1, order, 8, level);

      // try place fuel, or leave alone if there is already fuel
      if (!fuel.subStacks().isEmpty()) {
         ItemStack newInput = fuel.aggregateStack();
         if (furnaceBlockEntity.getItem(1).isEmpty() && furnaceBlockEntity.canPlaceItem(1, newInput)) {
            furnaceBlockEntity.setItem(1, newInput);
            pendingOutput.consumeIngredients(List.of(fuel));
            useSuccess = true;
         }
      }

      // try place cooking ingredients, either by adding to the existing item, or replacing it
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
            if (ContainerHelper.addItemNicely(worksiteChests, currentlyInFurnace).isEmpty()) {
               furnaceBlockEntity.setItem(0, newInput);
               pendingOutput.consumeIngredients(toSmelt);
               useSuccess = true;
            }
         }
         // leave alone if it's the same item type already in the furnace
      }

      if (useSuccess) {
         villager.swing(InteractionHand.MAIN_HAND, true);
         villager.addWorkExhaustion(1);
         getBehaviourCooldowns().startCooldown(Cooldowns.START, Duration.of(7, ChronoUnit.SECONDS), gameTime);
      }

      doStop(level, villager, gameTime);
   }
}
