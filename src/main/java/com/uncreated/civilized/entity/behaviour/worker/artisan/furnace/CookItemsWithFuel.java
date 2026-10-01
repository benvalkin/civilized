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

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.behaviour.ArtisanHouseBehaviour;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TransferToBuildingInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.BuildingStockRequirement;
import com.uncreated.civilized.core.building.production.PendingProduction;
import com.uncreated.civilized.core.building.production.PendingProductionOutput;
import com.uncreated.civilized.core.building.production.RecipeProductionSystem;
import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.building.production.bills.RecipeSlotType;
import com.uncreated.civilized.core.building.production.lines.singleitem.SingleItemRecipeOrder;
import com.uncreated.civilized.core.building.production.lines.singleitem.cooking.CookingMachine;
import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class CookItemsWithFuel extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private AbstractFurnaceBlockEntity furnaceBlockEntity;
   private MediumDistanceTravelTask travelHelper;

   private CookingMachine cookingMachine;
   private Notification missingIngredientsNotification;

   public CookItemsWithFuel(BehaviourState state) {
      super(state, true, false, 60 * 20, 20);
   }

   private enum FurnaceUsageDecision {
      LET_THEM_COOK, EXTRACT_COOKED_GOODS, EXTRACT_EVERYTHING, PLACE_NEW_ITEMS, IMPORT_MISSING_ITEMS
   }

   private FurnaceUsageDecision decision;

   /**
    * A furnace's slots hold up to a stack each.
    */
   private static final int FUEL_SLOT_CAPACITY = 64;
   /**
    * How long a furnace takes per item, used if the ingredient's recipe can't be found.
    */
   private static final int DEFAULT_COOKING_TIME = 200;

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

         if (!(blockEntity instanceof AbstractFurnaceBlockEntity f))
            return;

         if (isCorrectFurnaceBlock(f)) {
            this.furnaceBlockEntity = f;
            traversal.terminate();
         }

         if (level.canSeeSky(traversal.getCurrentBlockPos()))
            traversal.skipToNextXZ();
      });

      if (furnaceBlockEntity == null)
         return false;

      if (!(getWorksite().getBehaviour() instanceof ArtisanHouseBehaviour behaviour))
         return false;

      cookingMachine = getProductionMachine(behaviour.getRecipeProductionSystem());

      List<Container> worksiteChests = getWorksite().chests();
      List<Container> storehouseChests = storehouse.map(LoadedBuilding::chests).orElse(List.of());
      List<Container> storehouseAndWorksiteChests =
            Stream.concat(worksiteChests.stream(), storehouseChests.stream()).toList();

      if (isCooking(level)) {

         if (canTakeFromSlot(villager.getWorkOutputInventory(), 2)) {
            decision = FurnaceUsageDecision.EXTRACT_COOKED_GOODS;
            return true;
         }

         decision = FurnaceUsageDecision.LET_THEM_COOK;
         return false; // the current order is cooking. there is nothing take out yet. let it cook.
      } else if (!furnaceIsCompletelyEmpty()) {
         // we are no longer cooking, or it's time for another production bill.
         // go to the furnace and extract everything so that we can decide what to next
         decision = FurnaceUsageDecision.EXTRACT_EVERYTHING;
         return true;
      }

      Optional<PendingProduction> nextOrder =
            cookingMachine.tryAdvanceToProcessableOrder(worksiteChests, storehouseAndWorksiteChests);

      boolean atLeast1FuelItemPresent = false;
      if (nextOrder.isPresent())
         atLeast1FuelItemPresent =
               !nextOrder.get().output().getConsumableFuel(1, nextOrder.get().order(), 1, level).isEmpty();

      if (nextOrder.isPresent() && atLeast1FuelItemPresent) {
         decision = FurnaceUsageDecision.PLACE_NEW_ITEMS;
         return true; // the next order already has ingredients in worksite chests. We can go ahead and start processing
         // it.
      }

      decision = FurnaceUsageDecision.IMPORT_MISSING_ITEMS;

      // if we are here, we are either missing ingredients or fuel.
      // we need to import them from the storehouse
      if (storehouse.isEmpty())
         return false; // cannot import anything if the storehouse doesn't exist

      List<BuildingStockRequirement> allRecipeStockRequirements =
            createIngredientsRequirementsForAllRecipes(storehouseAndWorksiteChests, level);
      // ensure that no-one can take away ingredients from this building, including this villager when taking items
      // for other activities (e.g. offloading home items at the storehouse)
      reserveRequiredItems(villager, "smelting_production", getWorksite(), allRecipeStockRequirements);

      String party = reservationPartyKey(villager);

      Optional<TransferToBuildingInstruction> fetchFromStorehouse =
            TransferToBuildingInstruction.createIfAnyMetFromSourceBuildings(
                  villager,
                  new ReservationKey(party, "cooking_ingredients"),
                  allRecipeStockRequirements,
                  getWorksite(),
                  List.of(storehouse.get()));
      if (fetchFromStorehouse.isPresent()) {
         villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), fetchFromStorehouse.get());
         getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
         getStateMachine().queueActionOnce(this.getState());
      } else if (!allRecipeStockRequirements.isEmpty()) {
         // there are requirements for bills that we aren't able to fetch from anywhere at the moment
         int randomIndex = villager.getRandom().nextInt(allRecipeStockRequirements.size());
         ItemStack icon = allRecipeStockRequirements.get(randomIndex).getDisplayItem();
         missingIngredientsNotification =
               Notification.missingIngredients("missing_cooking_ingredients", villager.getInfo(), icon.getItem())
                     .build();
         NotificationService.INSTANCE.sendNotification(missingIngredientsNotification);
      }

      return false;
   }

   private boolean canTakeFromSlot(SimpleContainer villagerInventory, int slot) {
      ItemStack cookedGoods = furnaceBlockEntity.getItem(slot);
      if (cookedGoods.isEmpty())
         return false;

      return furnaceBlockEntity.canTakeItem(villagerInventory, slot, cookedGoods);
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
                  amount,
                  productionOrder.getBill().getDisplayItem());
      // makes it so that duplicate ingredients are still taken
      requirement.disregardExistingCarriedStock(true);

      return requirement;
   }

   protected abstract CookingMachine getProductionMachine(RecipeProductionSystem recipeProductionSystem);

   protected abstract RecipeType<? extends AbstractCookingRecipe> getRecipeType();

   protected abstract boolean isCorrectFurnaceBlock(AbstractFurnaceBlockEntity furnaceBlockEntity);

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);

      if (getRecipeType() != cookingMachine.getProductionType().recipeType())
         throw new IllegalStateException("getRecipeType and CookingMachine's recipeType do not match.");

      NotificationService.INSTANCE.resolveNotification(missingIngredientsNotification);
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

      if (furnaceBlockEntity.isRemoved()) {
         doStop(level, villager, gameTime);
         return;
      }

      villager.getBrain()
            .setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(furnaceBlockEntity.getBlockPos()));

      if (decision == FurnaceUsageDecision.EXTRACT_COOKED_GOODS) {
         extractFurnaceItems(villager, 2);
         dumpInventoryToChests(villager.getWorkOutputInventory(), getWorksite().chests());
         startUsageCooldown(gameTime);
      } else if (decision == FurnaceUsageDecision.EXTRACT_EVERYTHING) {
         extractFurnaceItems(villager, 0);
         extractFurnaceItems(villager, 1);
         extractFurnaceItems(villager, 2);
         dumpInventoryToChests(villager.getWorkOutputInventory(), getWorksite().chests());
      } else if (decision == FurnaceUsageDecision.PLACE_NEW_ITEMS) {

         Optional<LoadedBuilding> storehouse = findStorehouse(getSettlement());
         if (storehouse.isEmpty()) {
            doStop(level, villager, gameTime);
            return;
         }

         List<Container> worksiteChests = getWorksite().chests();
         List<Container> storehouseChests = storehouse.map(LoadedBuilding::chests).orElse(List.of());
         List<Container> storehouseAndWorksiteChests =
               Stream.concat(worksiteChests.stream(), storehouseChests.stream()).toList();

         Optional<PendingProduction> nextOrder =
               cookingMachine.tryAdvanceToProcessableOrder(worksiteChests, storehouseAndWorksiteChests);
         if (nextOrder.isEmpty()) {
            // for some reason, there was nothing to cook, yet there was when we started this behaviour.
            // startVisitCooldown(gameTime);
            doStop(level, villager, gameTime);
            return;
         }

         PendingProductionOutput pending = nextOrder.get().output();
         PendingProductionOutput.ConsumableIngredientStack ingredient = pending.getConsumableIngredients().getFirst();

         // asking for a whole stack shows which fuel would be used, then only as much of it as the batch needs is kept.
         // Asking for fewer could pick a worse fuel, since the search stops as soon as it has found enough
         PendingProductionOutput.ConsumableIngredientStack availableFuel =
               pending.getConsumableFuel(1, nextOrder.get().order(), FUEL_SLOT_CAPACITY, level);
         int fuelNeeded = fuelNeededToCook(ingredient.aggregateStack(), availableFuel.aggregateStack(), level);
         PendingProductionOutput.ConsumableIngredientStack fuel = limitTo(availableFuel, fuelNeeded);

         // there shouldn't be anything in the furnace here, remove it just in case
         extractFurnaceItems(villager, 0);
         extractFurnaceItems(villager, 1);
         extractFurnaceItems(villager, 2);

         if (!furnaceIsCompletelyEmpty()) { // just in case the villager's inventory is full
            doStop(level, villager, gameTime);
            return;
         }

         furnaceBlockEntity.setItem(0, ingredient.aggregateStack());
         furnaceBlockEntity.setItem(1, fuel.aggregateStack());

         pending.consumeIngredients(List.of(ingredient, fuel));

         startUsageCooldown(gameTime);
      }

      villager.swing(InteractionHand.MAIN_HAND, true);
      villager.addWorkExhaustion(1);

      doStop(level, villager, gameTime);
   }

   private int fuelNeededToCook(ItemStack ingredients, ItemStack fuel, ServerLevel level) {
      int burnDuration = getFuelBurnDuration(fuel, level);
      if (fuel.isEmpty() || burnDuration <= 0)
         return 1;

      int cookingTime =
            findCookingRecipe(getRecipeType(), new SingleRecipeInput(ingredients), level)
                  .map(recipe -> recipe.value().cookingTime())
                  .orElse(DEFAULT_COOKING_TIME);

      // use ceilDiv to avoid losing the remainder which may result in choosing a fuel count that's slightly below what
      // we actually to cook the full stack
      int fuelNeeded = Math.ceilDiv(ingredients.getCount() * cookingTime, burnDuration);
      int fuelSlotLimit = Math.min(FUEL_SLOT_CAPACITY, fuel.getMaxStackSize());
      return Math.clamp(fuelNeeded, 1, fuelSlotLimit);
   }

   protected int getFuelBurnDuration(ItemStack fuel, ServerLevel level) {
      return fuel.getBurnTime(getRecipeType(), level.fuelValues());
   }

   /**
    * The first {@code count} items of the stack, taken from its sub-stacks in order.
    */
   private static PendingProductionOutput.ConsumableIngredientStack limitTo(
         PendingProductionOutput.ConsumableIngredientStack stack,
         int count) {
      PendingProductionOutput.ConsumableIngredientStack limited =
            new PendingProductionOutput.ConsumableIngredientStack();

      int remaining = count;
      for (PendingProductionOutput.SourceIngredientSubStack subStack : stack.subStacks()) {
         if (remaining <= 0)
            break;

         int taken = Math.min(remaining, subStack.itemStack().getCount());
         limited.add(
               new PendingProductionOutput.SourceIngredientSubStack(
                     subStack.itemStack().copyWithCount(taken),
                     subStack.sourceContainer(),
                     subStack.sourceContainerSlot()));
         remaining -= taken;
      }

      return limited;
   }

   private static <T extends AbstractCookingRecipe> Optional<RecipeHolder<T>> findCookingRecipe(
         RecipeType<T> recipeType,
         SingleRecipeInput input,
         ServerLevel level) {
      return level.recipeAccess().getRecipeFor(recipeType, input, level);
   }

   public boolean isCooking(ServerLevel level) {
      if (!isBurning(level))
         return false;

      ItemStack input = furnaceBlockEntity.getItem(0);
      if (input.isEmpty())
         return false;

      SingleRecipeInput recipeInput = new SingleRecipeInput(input);
      Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipe =
            findCookingRecipe(getRecipeType(), recipeInput, level);
      if (recipe.isEmpty())
         return false;

      // the result has to fit in the output slot, or the furnace stalls
      ItemStack result = recipe.get().value().assemble(recipeInput, level.registryAccess());
      ItemStack output = furnaceBlockEntity.getItem(2);
      if (output.isEmpty())
         return true;

      int maxStackSize = Math.min(furnaceBlockEntity.getMaxStackSize(), output.getMaxStackSize());
      return ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= maxStackSize;
   }

   public boolean isBurning(Level level) {
      return level.getBlockState(furnaceBlockEntity.getBlockPos()).getValue(AbstractFurnaceBlock.LIT);
   }

   private void startUsageCooldown(long gameTime) {
      getBehaviourCooldowns().startCooldown(Cooldowns.START, Duration.of(15, ChronoUnit.SECONDS), gameTime);
   }

   protected boolean furnaceIsCompletelyEmpty() {
      return furnaceBlockEntity.isEmpty();
   }

   protected boolean extractFurnaceItems(CivilizedVillager villager, int furnaceSlot) {
      ItemStack item = furnaceBlockEntity.getItem(furnaceSlot);
      if (item.isEmpty())
         return false;
      if (!furnaceBlockEntity.canTakeItem(villager.getWorkOutputInventory(), furnaceSlot, item))
         return false;

      ItemStack remainder = villager.getWorkOutputInventory().addItem(item);
      furnaceBlockEntity.setItem(furnaceSlot, remainder);
      int taken = item.getCount() - remainder.getCount();

      if (furnaceSlot == 2) {
         villager.addWorkExhaustion(taken);
         cookingMachine.consumeTokens(taken);
      } else if (taken > 0)
         villager.addWorkExhaustion(1);

      return true;
   }
}
