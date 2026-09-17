package com.uncreated.civilized.entity.behaviour.worker.rancher;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.orders.StorehouseOrder;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportUpTo;
import com.uncreated.civilized.core.building.logistics.orders.task.TaskConsumableItemRequirement;
import com.uncreated.civilized.core.building.state.animalfarm.AnimalFarmState;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class BreedAnimals<T extends Animal> extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private MediumDistanceTravelTask travelHelper;

   private ItemStack handHeld;

   public BreedAnimals() {
      super(WorkStates.BREEDING_ANIMALS, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      List<Animal> breedableAnimals = getBreedableAnimals(level);
      if (breedableAnimals.size() < 2)
         return false;

      List<Animal> totalAnimals = getTotalAnimals(level);
      if (totalAnimals.size() > 8)
         return false;

      LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();

      AnimalFarmState behaviour = (AnimalFarmState) getWorksite().getBuilding().getState();
      TaskConsumableItemRequirement taskItemRequirement =
            new TaskConsumableItemRequirement(
                  level,
                  "breed_animals",
                  behaviour::isCorrectFood,
                  StorehouseOrder.Origin.AUTOMATIC,
                  2,
                  8);
      taskItemRequirement.setExpiry(12000);
      logisticsManager.registerOrder(getHome().getBuilding(), taskItemRequirement);
      ImportUpTo importOrder =
            new ImportUpTo(
                  level,
                  "animal_food",
                  taskItemRequirement.getItemSearch(),
                  StorehouseOrder.Origin.AUTOMATIC,
                  1,
                  8,
                  8);
      importOrder.setExpiry(12000);
      logisticsManager.registerOrder(getHome().getBuilding(), importOrder);

      List<ItemStack> animalFoodItemsInventory = getAnimalFoodItemsInventory(villager, breedableAnimals.getFirst());
      if (animalFoodItemsInventory.stream().mapToInt(ItemStack::getCount).sum() < 2) {
         getStateMachine().queueActionOnce(WorkStates.FETCHING_WORK_INPUT_FROM_HOME);
         getStateMachine().queueActionOnce(this.getState());
         return false;
      }
      this.handHeld = animalFoodItemsInventory.getFirst();

      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      AABB tooCloseBounds = getWorksite().getBuilding().getBounds().getEncapsulatingAABB();
      AABB closeEnoughBounds = tooCloseBounds.inflate(4);

      travelHelper =
            new MediumDistanceTravelTask(
                  villager,
                  getWorksite().getBuilding().getBlockPos(),
                  (v, d, closEnough) -> closeEnoughBounds.contains(v.position()),
                  Math.max((int) closeEnoughBounds.getXsize() / 2, (int) closeEnoughBounds.getZsize() / 2));

      villager.setItemSlot(EquipmentSlot.MAINHAND, handHeld);
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

      if (gameTime - lastWorkTime > 20) {

         lastWorkTime = gameTime;

         Optional<Animal> animal = getBreedableAnimals(level).stream().findAny();
         if (animal.isEmpty()) {
            doStop(level, villager, gameTime);
            return;
         }

         Optional<ItemStack> animalFoodItemsInventory =
               getAnimalFoodItemsInventory(villager, animal.get()).stream().findAny();
         if (animalFoodItemsInventory.isEmpty()) {
            doStop(level, villager, gameTime);
            return;
         }

         villager.swing(InteractionHand.MAIN_HAND, true);

         animal.get().setInLove(null);
         villager.getWorkInputInventory().removeItemType(animalFoodItemsInventory.get().getItem(), 1);
      }
   }

   protected List<Animal> getBreedableAnimals(ServerLevel level) {
      return level.getEntitiesOfClass(
            Animal.class,
            getWorksite().getBuilding().getBounds().getEncapsulatingAABB(),
            a -> !a.isBaby() && !a.isInLove() && a.canFallInLove() && a.getAge() == 0);
   }

   protected List<Animal> getTotalAnimals(ServerLevel level) {
      return level.getEntitiesOfClass(Animal.class, getWorksite().getBuilding().getBounds().getEncapsulatingAABB());
   }

   private List<ItemStack> getAnimalFoodItemsInventory(CivilizedVillager villager, Animal animal) {
      return villager.getWorkInputInventory().getItems().stream().filter(animal::isFood).toList();
   }
}
