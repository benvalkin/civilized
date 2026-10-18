package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TakeToInventoryInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.EquipmentRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.core.building.production.bills.ItemFilter;
import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.settlement.loadouts.ServerLoadoutsStore;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.Cooldowns;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.entity.equipment.VillagerEquipment;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class CheckForNewEquipment extends StatefulBehaviour {

   public CheckForNewEquipment() {
      super(WorkStates.CHECKING_FOR_NEW_EQUIPMENT, 0, 120 * 20);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      return false; // stop immediately (once-off behaviour)
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      if (getBehaviourCooldowns().hasCooldown(Cooldowns.CHECK_START, level.getGameTime()))
         return false;

      getBehaviourCooldowns()
            .startCooldown(Cooldowns.CHECK_START, Duration.of(15, ChronoUnit.SECONDS), level.getGameTime());
      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {

      Optional<Loadout> loadout = ServerLoadoutsStore.INSTANCE.find(villager.getInfo().getLoadoutId());
      if (loadout.isEmpty())
         return;

      Optional<LoadedBuilding> home = LoadedBuildings.checkLoaded(villager.getInfo().getHomeBuildingId());
      Optional<LoadedBuilding> storehouse =
            ServerBuildingsStore.INSTANCE.findStorehouse(villager.getInfo().getSettlementId())
                  .flatMap(LoadedBuildings::checkLoaded);
      // todo: probably should add military buildings to this list later on

      List<LoadedBuilding> weaponStockBuildings = Stream.of(storehouse, home).flatMap(Optional::stream).toList();

      List<EquipmentRequirement> newEquipment = invalidateEquipment(loadout.get(), weaponStockBuildings, villager);
      if (!villager.getEquipment().getOverflowInventory().isEmpty()) {
         // drop off old equipment first before taking anything new - this is to free up old equipment for other
         // villagers that might need it and to make sure all unused equipment is back in storage so that further
         // invalidation checks can detect it
         if (weaponStockBuildings.isEmpty())
            return;

         DropOffItemsInstruction dropOffItemsAtBuilding =
               new DropOffItemsInstruction(
                     weaponStockBuildings.getFirst(),
                     List.of(v -> v.getEquipment().getOverflowInventory()));
         villager.getBrain().setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), dropOffItemsAtBuilding);
         getStateMachine().queueImmediately(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);

         // reset because we'll be checking for new equipment shortly after dropping off unused equipment
         getBehaviourCooldowns().resetCooldown(Cooldowns.CHECK_START);
      } else if (!newEquipment.isEmpty()) {
         String party = reservationPartyKey(villager);
         ReservationKey reservationKey = new ReservationKey(party, "take_equipment");
         Optional<TakeToInventoryInstruction> takeEquipmentInstruction =
               TakeToInventoryInstruction.createIfAnyMetFromSourceBuildings(
                     villager,
                     reservationKey,
                     InventoryStockRequirement.cast(newEquipment),
                     weaponStockBuildings);
         if (takeEquipmentInstruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), takeEquipmentInstruction.get());
            getStateMachine().queueImmediately(WorkStates.TAKING_ITEMS_TO_INVENTORY);
         }
      }
   }

   private List<EquipmentRequirement> invalidateEquipment(
         Loadout loadout,
         List<LoadedBuilding> stockBuildings,
         CivilizedVillager villager) {

      VillagerEquipment equipment = villager.getEquipment();
      String party = reservationPartyKey(villager);

      List<EquipmentRequirement> newEquipment = new LinkedList<>();

      // The design of this method centers around FIRST removing all inferior/disallowed equipment (so that it can be
      // deposited back in building storage) if there's upgrade available, and THEN trying to take the best possible
      // equipment from building storage.
      //
      // The process must be done this order primarily because the hauling system does not have any support for swapping
      // out carried items during hauls. It also helps separate the concerns of each step and makes detecting inferior
      // equipment/picking up superior equipment a lot simpler.

      for (CarriedEquipmentSlot slot : Loadout.SUPPORTED_SLOTS) {

         Optional<ItemFilter> allowedItems = loadout.getAllowedItems(slot);
         ItemStack currentEquipment = equipment.getEquippedOrCarried(slot);

         if (allowedItems.isEmpty()) {
            // this loadout does not permit any items to be carried in this slot
            if (!currentEquipment.isEmpty())
               equipment.moveOutCurrentEquipment(slot);

            continue;
         }

         ItemFilter allowedSanitized = allowedItems.get().sanitized();

         boolean equipmentNotAllowedByLoadout =
               !currentEquipment.isEmpty() && !allowedSanitized.acceptsItem(currentEquipment);
         if (equipmentNotAllowedByLoadout) {
            equipment.moveOutCurrentEquipment(slot);
            continue;
         }

         EquipmentRequirement allowedEquipment = EquipmentRequirement.allowedBy(slot, allowedSanitized);
         ItemStockRequirement.StockResult bestEquipmentStock = allowedEquipment.evaluate(party, stockBuildings);

         if (!bestEquipmentStock.totalStock().hasItems())
            continue; // no stock equipment available - even worse items

         // evaluate orders total stock by preference, so we can guarantee that the best possible item is always the
         // first in the stock result.
         ItemStack bestEquipment = bestEquipmentStock.totalStock().getItemStacks().getFirst();

         int currentEquipmentScore = allowedSanitized.preferenceIndex(currentEquipment);
         int bestEquipmentScore = allowedSanitized.preferenceIndex(bestEquipment);

         boolean betterEquipmentAvailable = currentEquipment.isEmpty() || bestEquipmentScore < currentEquipmentScore;

         if (betterEquipmentAvailable) {
            equipment.moveOutCurrentEquipment(slot);
            EquipmentRequirement onlyBestEquipment =
                  EquipmentRequirement.onlyBestPossibleEquipment(slot, allowedSanitized, bestEquipment);
            newEquipment.add(onlyBestEquipment);
         }
      }

      return newEquipment;
   }
}
