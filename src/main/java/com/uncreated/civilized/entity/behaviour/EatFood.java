package com.uncreated.civilized.entity.behaviour;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TakeToInventoryInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Eats food from its logistics inventory, one item at a time, until it is no longer hungry. When it has no food on it,
 * it first fetches some from a viable building (food vendor, home or storehouse).
 */
@Log4j2
public class EatFood extends StatefulBehaviour {

   private static final String FOOD_RESERVATION_NAME = "food";
   private static final int PARTICLES_PER_BITE = 5;

   /** Fetching one item at a time keeps villagers from hoarding food that others in the settlement could eat. */
   private static final InventoryStockRequirement FOOD_REQUIREMENT =
         new InventoryStockRequirement("food", EatFood::isViableFood, 1, 1, VillagerInventoryType.LOGISTICS);

   /** The logistics inventory slot of the food being eaten, or -1 when the villager is between items. */
   private int eatingSlot = -1;
   private int eatingTicksRemaining;

   public EatFood(BehaviourState state) {
      super(state);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      if (!villager.getHunger().isHungry())
         return false;

      if (findMostSatiatingFood(villager.getLogisticsInventory()) != -1)
         return true;

      tryFetchFood(villager);
      return false;
   }

   private void tryFetchFood(CivilizedVillager villager) {
      Set<Building> buildings = ServerBuildingsStore.INSTANCE.findForSettlement(villager.getInfo().getSettlementId());

      List<LoadedBuilding> foodSources =
            buildings.stream()
                  .filter(
                        b -> b.getBuildingType().isFoodVendor()
                              || b.getBuildingId().equals(villager.getInfo().getHomeBuildingId())
                              || b.getBuildingType().is(BuildingTypes.STOREHOUSE))
                  // prefer closest buildings
                  .sorted(
                        Comparator.comparingDouble(
                              b -> b.getBounds().getEncapsulatingAABB().distanceToSqr(villager.position())))
                  // prefer food vendor buildings over others
                  .sorted(Comparator.comparing(b -> !b.getBuildingType().isFoodVendor()))
                  // only loaded buildings can be taken from
                  .map(LoadedBuildings::checkLoaded)
                  .flatMap(Optional::stream)
                  .toList();

      if (foodSources.isEmpty())
         return; // can't fetch food if there is nowhere to fetch it from

      // note: there isn't a way to prefer more satiating foods here yet. A "preference" system will probably come next.
      Optional<TakeToInventoryInstruction> instruction =
            TakeToInventoryInstruction.createIfMetFromSourceBuildings(
                  new ReservationKey(reservationPartyKey(villager), FOOD_RESERVATION_NAME),
                  FOOD_REQUIREMENT,
                  foodSources);

      if (instruction.isEmpty())
         return; // none of the buildings have food in stock

      villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), instruction.get());
      getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
      getStateMachine().queueActionOnce(this.getState());
   }

   private static boolean isViableFood(ItemStack i) {
      return i.is(Tags.Items.FOODS) && !i.is(Tags.Items.FOODS_RAW_FISH) && !i.is(Tags.Items.FOODS_RAW_MEAT);
   }

   private static int findMostSatiatingFood(SimpleContainer inventory) {
      int bestSlot = -1;
      FoodProperties bestFood = null;

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (!isViableFood(stack))
            continue;

         FoodProperties food = stack.get(DataComponents.FOOD);
         if (food == null)
            continue;

         if (bestFood == null || food.nutrition() > bestFood.nutrition()
               || (food.nutrition() == bestFood.nutrition() && food.saturation() > bestFood.saturation())) {
            bestSlot = i;
            bestFood = food;
         }
      }

      return bestSlot;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      eatingSlot = -1;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      eatingSlot = -1;
      villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      SimpleContainer inventory = villager.getLogisticsInventory();

      if (eatingSlot == -1) {
         // done with the last item: keep going only while still hungry and there is food left
         if (!villager.getHunger().isHungry() || !startEatingNextFood(villager, inventory)) {
            doStop(level, villager, gameTime);
            return;
         }
      }

      ItemStack food = inventory.getItem(eatingSlot);
      Consumable consumable = food.get(DataComponents.CONSUMABLE);
      if (!isViableFood(food) || consumable == null) {
         // the food was taken out of the inventory while being eaten
         eatingSlot = -1;
         return;
      }

      eatingTicksRemaining--;

      if (consumable.shouldEmitParticlesAndSounds(eatingTicksRemaining))
         emitEatingEffects(level, villager, food, consumable);

      if (eatingTicksRemaining <= 0)
         finishEating(level, villager, inventory);
   }

   private boolean startEatingNextFood(CivilizedVillager villager, SimpleContainer inventory) {
      int slot = findMostSatiatingFood(inventory);
      if (slot == -1)
         return false;

      ItemStack foodItem = inventory.getItem(slot);
      Consumable consumable = foodItem.get(DataComponents.CONSUMABLE);
      eatingSlot = slot;
      eatingTicksRemaining =
            consumable != null ? consumable.consumeTicks() : (int) (Consumable.DEFAULT_CONSUME_SECONDS * 20);
      villager.setItemInHand(InteractionHand.MAIN_HAND, foodItem);
      return true;
   }

   private void finishEating(ServerLevel level, CivilizedVillager villager, SimpleContainer inventory) {
      ItemStack food = inventory.getItem(eatingSlot);
      FoodProperties foodProperties = food.get(DataComponents.FOOD);

      // this applies the food's own effects (e.g. golden apples), plays the final sound, and uses up one item. Whatever
      // it leaves behind, such as a bowl from stew, takes the food's place in the inventory
      ItemStack leftover = food.finishUsingItem(level, villager);
      inventory.setItem(eatingSlot, leftover);

      if (foodProperties != null)
         villager.getHunger().eat(foodProperties);

      eatingSlot = -1;
   }

   private static void emitEatingEffects(
         ServerLevel level,
         CivilizedVillager villager,
         ItemStack food,
         Consumable consumable) {
      // no particles are spawned on the server by this, only the eating sound is played
      consumable.emitParticlesAndSounds(villager.getRandom(), villager, food, 0);

      if (!consumable.hasConsumeParticles())
         return;

      Vec3 mouth = villager.getEyePosition().add(villager.getLookAngle().scale(0.4)).subtract(0, 0.15, 0);
      level.sendParticles(
            new ItemParticleOption(ParticleTypes.ITEM, food),
            mouth.x,
            mouth.y,
            mouth.z,
            PARTICLES_PER_BITE,
            0.1,
            0.05,
            0.1,
            0.05);
   }
}
