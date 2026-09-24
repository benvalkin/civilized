package com.uncreated.civilized.entity.behaviour.worker.rancher;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TakeToInventoryInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.state.animalfarm.AnimalFarmState;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

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
   private Class<? extends Animal> animalFarmMobType;

   public BreedAnimals() {
      super(WorkStates.BREEDING_ANIMALS, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      animalFarmMobType = getWorksite().getBuilding().getBuildingType().animalFarmMobType();
      Objects.requireNonNull(animalFarmMobType, "worksite is expected to be an animal farm");

      List<Animal> totalAnimals = getTotalAnimals(level, animalFarmMobType);
      if (totalAnimals.size() > 8)
         return false;

      String party = reservationPartyKey(villager);

      AnimalFarmState animalFarmState = (AnimalFarmState) getWorksite().getBuilding().getState();
      InventoryStockRequirement animalFoodRequirement = animalFarmState.getAnimalFoodRequirement();

      if (totalAnimals.size() > 2) {
         // if there enough total animals to breed, reserve the minimum viable animal food at the storehouse
         findStorehouse(getSettlement()).ifPresent(storehouse -> {
            storehouse.placeReservation(
                  new ReservationKey(party, animalFoodRequirement.key() + "_static"),
                  animalFoodRequirement.filter(),
                  animalFoodRequirement.minimumAcceptableAmount());
         });
      }

      List<Animal> breedableAnimals = getBreedableAnimals(level, animalFarmMobType);
      if (breedableAnimals.size() < 2) {
         return false;
      }

      InventoryStockRequirement.StockResult carrying = animalFoodRequirement.evaluate(villager);
      if (!carrying.satisfied()) {
         Optional<TakeToInventoryInstruction> instruction =
               TakeToInventoryInstruction.createIfMetFromSourceBuildings(
                     new ReservationKey(party, animalFoodRequirement.key()),
                     animalFoodRequirement,
                     homeAndStorehouseIfPresent());
         if (instruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), instruction.get());
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            getStateMachine().queueActionOnce(this.getState());
            // todo: send notification that the villager is missing shears
         }

         return false;
      }

      this.handHeld = carrying.stock().getItemStacks().getFirst();
      assert this.handHeld.getCount() >= 2;
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

      if (travelHelper.isJourneySuccessful())
         pickUpDroppedItemsAtWorksite(level, villager);
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

         Optional<Animal> animal = getBreedableAnimals(level, animalFarmMobType).stream().findAny();
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
         villager.addWorkExhaustion(1);

         animal.get().setInLove(null);
         villager.getWorkInputInventory().removeItemType(animalFoodItemsInventory.get().getItem(), 1);
      }
   }

   protected List<Animal> getBreedableAnimals(ServerLevel level, Class<? extends Animal> animalFarmMobType) {
      return level.getEntitiesOfClass(
            Animal.class,
            getWorksite().getBuilding().getBounds().getEncapsulatingAABB(),
            a -> animalFarmMobType.isInstance(a) && !a.isBaby() && !a.isInLove() && a.canFallInLove()
                  && a.getAge() == 0);
   }

   protected List<Animal> getTotalAnimals(ServerLevel level, Class<? extends Animal> animalFarmMobType) {
      return level.getEntitiesOfClass(
            Animal.class,
            getWorksite().getBuilding().getBounds().getEncapsulatingAABB(),
            animalFarmMobType::isInstance);
   }

   private List<ItemStack> getAnimalFoodItemsInventory(CivilizedVillager villager, Animal animal) {
      return villager.getWorkInputInventory().getItems().stream().filter(animal::isFood).toList();
   }
}
