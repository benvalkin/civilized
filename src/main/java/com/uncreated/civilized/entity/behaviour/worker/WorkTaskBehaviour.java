package com.uncreated.civilized.entity.behaviour.worker;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.entity.LoadedBuildings;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.DropOffItemsInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ItemStockRequirement;
import com.uncreated.civilized.core.building.util.WorkAreaUtil;
import com.uncreated.civilized.core.notifications.Delay;
import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.NotificationService;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.entity.LoadedVillagers;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourState;
import com.uncreated.civilized.entity.behaviour.BehaviourStateMachine;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviour;
import com.uncreated.civilized.entity.behaviour.StatefulBehaviourControl;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public abstract class WorkTaskBehaviour extends StatefulBehaviour {

   /**
    * How long a villager waits for its spouse to deliver what it was asked to fetch, before it would fetch the same
    * items again. A spouse that hasn't made it back by then may end up bringing a few more items than were needed.
    */
   protected static final Duration AWAITING_DELIVERY_DURATION = Duration.of(2, ChronoUnit.MINUTES);

   /** Whether this behaviour needs some sort of worksite in order to start. */
   private final boolean requiresWorksite;

   @Getter
   private LoadedSettlement settlement;

   /**
    * The place where the villagers actually performs work tasks. Can be the same building as their Work Building, or
    * their separate Work Area building.
    */
   public LoadedBuilding getWorksite() {
      if (!requiresWorksite)
         throw new UnsupportedOperationException(
               "Cannot get villager's worksite. This behaviour is not configured to make use of the villager's worksite ('requiresWorksite' is set to 'false')");

      return worksite;
   }

   /**
    * The main worker building where this worker is assigned. Their occupation originate from this building. All workers
    * must have a Work Building
    */
   public LoadedBuilding getWorkBuilding() {
      if (workBuilding == null)
         throw new IllegalStateException(
               "Cannot get villager's work building, either because they don't have one or it's not loaded. All workers must have a work building that is loaded in.");

      return workBuilding;
   }

   @Nullable
   private Notification missingItemNotification;
   @Nullable
   private Notification noWorkSiteNotification;

   @Nullable
   protected LoadedBuilding worksite;
   @Nullable
   private LoadedBuilding workBuilding;

   public WorkTaskBehaviour(BehaviourState state, boolean requiresWorksite, int duration, int cooldownDuration) {
      this(state, requiresWorksite, duration, duration, cooldownDuration);
   }

   public WorkTaskBehaviour(
         BehaviourState state,
         boolean requiresWorksite,
         int minDuration,
         int maxDuration,
         int cooldownDuration) {
      super(state, minDuration, maxDuration, cooldownDuration);
      this.requiresWorksite = requiresWorksite;
   }

   protected void notifyMissingItem(Notification.NotificationBuilder notification) {
      missingItemNotification = notification.build();
      NotificationService.INSTANCE.sendNotification(
            missingItemNotification,
            Delay.of(Duration.of(10, ChronoUnit.SECONDS), p -> this.getStatus() != Status.STOPPED));
   }

   protected void notifyMissingItem(Notification.NotificationBuilder notification, Duration delayDuration) {
      missingItemNotification = notification.build();
      NotificationService.INSTANCE.sendNotification(
            missingItemNotification,
            Delay.of(delayDuration, p -> this.getStatus() != Status.STOPPED));
   }

   protected void resolveMissingItemNotification() {
      NotificationService.INSTANCE.resolveNotification(missingItemNotification);
      missingItemNotification = null;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      VillagerInfo info = villager.getInfo();
      Optional<LoadedSettlement> loadedSettlement = LoadedSettlements.checkLoaded(info.getSettlementId());
      if (loadedSettlement.isEmpty()) {
         return false;
      }

      settlement = loadedSettlement.get();

      UUID worksiteId = info.getWorkBuildingId();
      workBuilding = LoadedBuildings.checkLoaded(worksiteId).orElse(null);
      // if the villager has no work building (they would be unemployed), or their work building is not loaded, they cannot
      // start this activity
      if (workBuilding == null)
         return false;

      if (!requiresWorksite)
         return true;

      if (info.getOccupation().workAreaTypes().isEmpty()) {
         // worksite is the work building
         worksite = workBuilding;
         return true;
      }

      // worksite is the work area
      Optional<Building> workArea = WorkAreaUtil.findWorkArea(info, ServerBuildingsStore.INSTANCE);
      if (workArea.isEmpty()) {
         noWorkSiteNotification = noWorkSiteNotification(info, info.getOccupation().workAreaTypes()).build();
         NotificationService.INSTANCE.sendNotification(
               noWorkSiteNotification,
               Delay.of(Duration.of(1, ChronoUnit.MINUTES), p -> villager.getBrain().isActive(Activity.WORK)));
         return false;
      }

      NotificationService.INSTANCE.resolveNotification(noWorkSiteNotification);
      noWorkSiteNotification = null;

      Optional<LoadedBuilding> loadedWorkArea = LoadedBuildings.checkLoaded(workArea.get().getBuildingId());
      if (loadedWorkArea.isEmpty())
         return false;

      worksite = loadedWorkArea.get();

      return true;
   }

   protected Optional<LoadedBuilding> findStorehouse(LoadedSettlement settlement) {
      return ServerBuildingsStore.INSTANCE.findStorehouse(settlement.getSettlement().getSettlementId())
            .flatMap(LoadedBuildings::checkLoaded);

   }

   /**
    * Tries to delegate the specified action to villager's spouse. If the spouse is unavailable (they must exist and be
    * free to run an errand), the villager will instead complete the action themselves. Useful for getting the
    * villager's spouse to do things (e.g. pick up resources) so that the villager doesn't have to.
    *
    * @param action
    *           given whichever villager ends up doing it, and that villager's state machine. Make sure to use those
    *           rather than this behaviour's villager and state machine.
    * @return whether the spouse is doing it
    */
   protected boolean tryDelegateToSpouse(
         CivilizedVillager villager,
         BiConsumer<CivilizedVillager, BehaviourStateMachine> action) {
      Optional<CivilizedVillager> freeSpouse = findSpouse(villager).filter(WorkTaskBehaviour::isFreeToHaul);
      if (freeSpouse.isPresent()) {
         Optional<StatefulBehaviourControl<?>> spouseBehaviourControl = freeSpouse.get().getCurrentBehaviourControl();
         if (spouseBehaviourControl.isPresent()) {
            action.accept(freeSpouse.get(), spouseBehaviourControl.get().getStateMachine());
            return true;
         }
      }

      action.accept(villager, getStateMachine());
      return false;
   }

   protected Optional<CivilizedVillager> findSpouse(CivilizedVillager villager) {
      UUID partnerId = villager.getInfo().getPartnerId();
      if (partnerId == null)
         return Optional.empty();

      return LoadedVillagers.find(partnerId)
            .filter(
                  spouse -> spouse.isAlive() && spouse.level() == villager.level()
                        && spouse.getInfo().isHomemaker());
   }

   private static boolean isFreeToHaul(CivilizedVillager villager) {
      if (!villager.getBrain().isActive(Activity.IDLE))
         return false;

      // not a good idea to interrupt a villager who is already doing something or minor importance
      boolean runningIdleTask =
            villager.getCurrentBehaviourControl().map(StatefulBehaviourControl::isRunningIdleTask).orElse(false);
      // not a good idea to interrupt a villager who is already fulfilling another hauling instruction
      boolean alreadyHasHaulingInstruction =
            villager.getBrain().hasMemoryValue(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get())
                  || villager.getBrain().hasMemoryValue(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get());
      return runningIdleTask && !alreadyHasHaulingInstruction;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
   }

   protected @NotNull List<LoadedBuilding> workBuildingAndStorehouseIfPresent() {
      List<LoadedBuilding> sourceBuildings = new ArrayList<>();
      sourceBuildings.add(getWorkBuilding());
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

            // if there is no remainder, we successfully inserted the item
            if (remainder.isEmpty()) {
               itemTypesDumped.add(itemType);
               break;
            }
         }
      }

      return itemTypesDumped;
   }

   protected void goDropOffWorkOutputAtWorkBuilding(CivilizedVillager villager) {
      DropOffItemsInstruction dropOffItemsInstruction =
            new DropOffItemsInstruction(getWorkBuilding(), List.of(CivilizedVillager::getWorkOutputInventory));
      villager.getBrain().setMemory(AIRegistry.MM_DROP_OFF_ITEMS_INSTRUCTION.get(), dropOffItemsInstruction);
      getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_ITEMS_AT_BUILDING);
   }

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
         villager.swing(InteractionHand.MAIN_HAND, true);
         villager.addWorkExhaustion(1);
      }

      return pickedUpAnything;
   }

   protected void reserveRequiredItems(
         CivilizedVillager villager,
         String reservationName,
         LoadedBuilding building,
         List<? extends ItemStockRequirement> requirements) {

      ReservationKey reservationKey = new ReservationKey(reservationPartyKey(villager), reservationName);
      reserveRequiredItems(reservationKey, building, requirements);
   }

   protected List<ItemReservation.Entry> reserveRequiredItems(
         ReservationKey reservationKey,
         LoadedBuilding building,
         List<? extends ItemStockRequirement> requirements) {

      if (requirements.isEmpty())
         return List.of();

      List<ItemReservation.Entry> entries =
            requirements.stream()
                  .map(requirement -> new ItemReservation.Entry(requirement.filter(), requirement.idealAmount()))
                  .toList();

      building.placeReservation(reservationKey, entries);
      return entries;
   }

   private static Notification.NotificationBuilder noWorkSiteNotification(
         VillagerInfo info,
         List<BuildingType> worksiteTypes) {

      if (info.getSettlementId() == null)
         throw new IllegalArgumentException("Villager's settlementId must not be null");

      Component headline = Component.translatable("notification.worker.no_worksite");

      Component detail;
      if (worksiteTypes.size() == 1)
         detail =
               Component.translatable(
                     "notification.worker.no_worksite.specific.detail",
                     info.getFirstName(),
                     info.getOccupation().translation(),
                     worksiteTypes.getFirst().translation());
      else
         detail =
               Component.translatable(
                     "notification.worker.no_worksite.detail",
                     info.getFirstName(),
                     info.getOccupation().translation());

      return Notification.workBlockedNotification("no_worksite", info, headline, detail, new ItemStack(Items.OAK_SIGN));
   }
}
