package com.uncreated.civilized.entity.behaviour.worker.common.logistics;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public abstract class ExchangeResourcesAtBuilding extends WorkTaskBehaviour {

   protected Building targetbuilding;
   private List<ChestBlockEntity> chestsAtTarget;
   private MediumDistanceTravelTask travelHelper;

   public ExchangeResourcesAtBuilding(BehaviourState workState, int duration, int cooldownDuration) {
      super(workState, true, true, duration, cooldownDuration);
   }

   protected abstract Optional<Building> findTargetBuilding(ServerLevel level, CivilizedVillager villager);

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      if (!super.checkExtraStartConditions(level, villager))
         return false;

      Optional<Building> targetBuilding = findTargetBuilding(level, villager);
      if (targetBuilding.isEmpty())
         return false;

      this.targetbuilding = targetBuilding.get();

      chestsAtTarget =
            this.targetbuilding.getBounds()
                  .getBlockEntitiesInsideBuilding(level)
                  .stream()
                  .filter(b -> b instanceof ChestBlockEntity)
                  .map(b -> (ChestBlockEntity) b)
                  .toList();

      if (chestsAtTarget.isEmpty())
         return false;

      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      // LOGGER.info("Villager going to exchange resources at {}.", targetbuilding.toStringLite());

      travelHelper = new MediumDistanceTravelTask(villager, targetbuilding.getBlockPos(), 3);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      // LOGGER.info("Villager finished exchange resources at {}.", targetbuilding.toStringLite());
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      exchangeResources(level, villager, tickTime);

      doStop(level, villager, tickTime);
   }

   protected abstract void exchangeResources(ServerLevel level, CivilizedVillager villager, long tickTime);

   protected Set<Item> dumpInventoryToChests(Container villagerInventory) {

      Set<Item> itemTypesDumped = new HashSet<>();

      for (int i = 0; i < villagerInventory.getContainerSize(); i++) {

         ItemStack item = villagerInventory.getItem(i);
         if (item.isEmpty())
            continue;

         Item itemType = item.getItem();

         for (var chest : chestsAtTarget) {
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

   protected int fillInventoryFromChests(Container villagerInventory, Predicate<ItemStack> itemSearch, int quota) {

      int transferRemaining = quota;
      for (var chest : chestsAtTarget) {

         int transferred = ContainerHelper.transferNicely(chest, villagerInventory, itemSearch, transferRemaining);

         transferRemaining -= transferred;

         if (transferRemaining <= 0)
            break;
      }

      return quota - transferRemaining;
   }

   protected boolean targetBuildingChestsHaveResources(Predicate<ItemStack> itemSearch) {
      for (var chest : chestsAtTarget) {
         if (chest.hasAnyMatching(itemSearch))
            return true;
      }
      return false;
   }
}
