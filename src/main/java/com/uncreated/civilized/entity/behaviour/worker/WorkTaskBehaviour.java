package com.uncreated.civilized.entity.behaviour.worker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.uncreated.civilized.util.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public abstract class WorkTaskBehaviour extends StatefulBehaviour {

   private final boolean requiresWorksite;
   private final boolean requiresHome;

   @Getter
   private LoadedSettlement settlement;

   public LoadedBuilding getWorksite() {
      if (!requiresWorksite)
         throw new UnsupportedOperationException(
               "Cannot get villager's worksite. This behaviour is not configured to make use of the villager's worksite ('requiresWorksite' is set to 'false')");

      return worksite;
   }

   public LoadedBuilding getHome() {
      if (!requiresHome)
         throw new UnsupportedOperationException(
               "Cannot get villager's home. This behaviour is not configured to make use of the villager's home ('requiresHome' is set to 'false')");

      return home;
   }

   @Nullable
   protected LoadedBuilding worksite;
   @Nullable
   private LoadedBuilding home;

   public WorkTaskBehaviour(
         BehaviourState state,
         boolean requiresWorksite,
         boolean requiresHome,
         int duration,
         int cooldownDuration) {
      super(state, duration, cooldownDuration);
      this.requiresWorksite = requiresWorksite;
      this.requiresHome = requiresHome;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      Optional<LoadedSettlement> loadedSettlement = LoadedSettlements.checkLoaded(villager.getInfo().getSettlementId());
      if (loadedSettlement.isEmpty())
         return false;

      settlement = loadedSettlement.get();

      if (requiresWorksite) {
         Optional<LoadedBuilding> building = LoadedBuildings.checkLoaded(villager.getInfo().getPrimaryWorksiteId());
         if (building.isEmpty())
            return false;

         worksite = building.get();
      }
      if (requiresHome) {
         Optional<LoadedBuilding> building = LoadedBuildings.checkLoaded(villager.getInfo().getHomeBuildingId());
         if (building.isEmpty())
            return false;

         home = building.get();
      }

      return true;
   }

   protected Optional<LoadedBuilding> findStorehouse(LoadedSettlement settlement) {
      return ServerBuildingsStore.INSTANCE.findStorehouse(settlement.getSettlement().getSettlementId())
            .flatMap(LoadedBuildings::checkLoaded);

   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
   }

   protected @NotNull List<LoadedBuilding> homeAndStorehouseIfPresent() {
      List<LoadedBuilding> sourceBuildings = new ArrayList<>();
      sourceBuildings.add(getHome());
      findStorehouse(getSettlement()).ifPresent(sourceBuildings::add);
      return sourceBuildings;
   }

   protected Set<Item> dumpInventoryToChests(Container villagerInventory, List<Container> chests) {

      Set<Item> itemTypesDumped = new HashSet<>();

      for (int i = 0; i < villagerInventory.getContainerSize(); i++) {

         ItemStack item = villagerInventory.getItem(i);
         if (item.isEmpty())
            continue;

         Item itemType = item.getItem();

         for (var chest : chests) {
            // try to add item to chest
            ItemStack remainder = ContainerHelper.addItemNicely(chest, item);
            villagerInventory.removeItem(i, item.getCount() - remainder.getCount());

            // if there is no remainder, we successfully inserted the stack
            if (remainder.isEmpty()) {
               itemTypesDumped.add(itemType);
               break;
            }
         }
      }

      return itemTypesDumped;
   }

   protected void goDropOffWorkOutputAtHome(CivilizedVillager villager) {
      // the home is looked up rather than taken from getHome(), so that behaviours which don't require a home can drop
      // off what they picked up too
      Optional<LoadedBuilding> homeBuilding = LoadedBuildings.checkLoaded(villager.getInfo().getHomeBuildingId());
      if (homeBuilding.isEmpty())
         return;

      DropOffItemsInstruction dropOffItemsInstruction =
            new DropOffItemsInstruction(homeBuilding.get(), List.of(VillagerInventoryType.WORK_OUTPUT));
      villager.getBrain().setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), dropOffItemsInstruction);
      getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);
   }

   /**
    * Collects items lying on the ground anywhere in the worksite into the villager's work output inventory, e.g. eggs
    * laid by chickens or drops that nobody picked up.
    *
    * @return whether anything was picked up
    */
   protected boolean pickUpDroppedItemsAtWorksite(ServerLevel level, CivilizedVillager villager) {

      List<ItemEntity> droppedItems =
            level.getEntitiesOfClass(
                  ItemEntity.class,
                  getWorksite().getBuilding().getBounds().getEncapsulatingAABB(),
                  i -> i.isAlive() && !i.hasPickUpDelay());

      boolean pickedUpAnything = false;
      for (ItemEntity droppedItem : droppedItems) {
         ItemStack remainder = villager.getWorkOutputInventory().addItem(droppedItem.getItem());

         if (remainder.getCount() == droppedItem.getItem().getCount())
            continue; // no room left in the villager's inventory

         pickedUpAnything = true;

         if (remainder.isEmpty())
            droppedItem.discard();
         else
            droppedItem.setItem(remainder);
      }

      if (pickedUpAnything) {
         villager.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
         villager.swing(InteractionHand.MAIN_HAND);
      }

      return pickedUpAnything;
   }
}
