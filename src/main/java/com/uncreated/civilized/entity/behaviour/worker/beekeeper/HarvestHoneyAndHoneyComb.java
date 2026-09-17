package com.uncreated.civilized.entity.behaviour.worker.beekeeper;

import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.orders.StorehouseOrder;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportUpTo;
import com.uncreated.civilized.core.building.logistics.orders.task.TaskConsumableItemRequirement;
import com.uncreated.civilized.core.building.logistics.orders.task.ToolRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Harvests full beehives at a bee farm. The villager needs shears to cut honeycomb, glass bottles to fill with honey,
 * or both, in which case it picks one of the two at random for every hive it harvests.
 */
public class HarvestHoneyAndHoneyComb extends WorkTaskBehaviour {

   private static final int HONEYCOMB_PER_HIVE = 3;
   private static final int MAX_BOTTLES = 4;
   private static final int WORK_INTERVAL_TICKS = 2 * 20;
   private static final int ORDER_EXPIRY_TICKS = 12000;

   private MediumDistanceTravelTask travelHelper;
   private long lastWorkTime;
   private @Nullable BlockPos nextFullBeehive;
   private boolean hasWorkOutputItems;

   public HarvestHoneyAndHoneyComb() {
      super(WorkStates.HARVESTING_HONEY, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      registerLogisticsOrders(level);

      findFullBeehive(level);
      if (nextFullBeehive == null)
         return false;

      if (findShears(villager).isEmpty() && findGlassBottle(villager).isEmpty()) {
         // todo: send notification that the villager is missing shears and bottles
         getStateMachine().queueActionOnce(WorkStates.FETCHING_WORK_INPUT_FROM_HOME);
         getStateMachine().queueActionOnce(this.getState());
         return false;
      }

      return true;
   }

   private void registerLogisticsOrders(ServerLevel level) {
      LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();

      ToolRequirement shearsRequirement =
            new ToolRequirement(level, "harvest_honey_comb", ShearsItem.class, StorehouseOrder.Origin.AUTOMATIC);
      shearsRequirement.setExpiry(ORDER_EXPIRY_TICKS);
      logisticsManager.registerOrder(getHome().getBuilding(), shearsRequirement);

      ImportUpTo shearsImport =
            new ImportUpTo(
                  level,
                  "shears",
                  shearsRequirement.getItemSearch(),
                  StorehouseOrder.Origin.AUTOMATIC,
                  1,
                  1,
                  1);
      shearsImport.setExpiry(ORDER_EXPIRY_TICKS);
      logisticsManager.registerOrder(getHome().getBuilding(), shearsImport);

      TaskConsumableItemRequirement bottlesRequirement =
            new TaskConsumableItemRequirement(
                  level,
                  "harvest_honey",
                  i -> i.is(Items.GLASS_BOTTLE),
                  StorehouseOrder.Origin.AUTOMATIC,
                  1,
                  MAX_BOTTLES);
      bottlesRequirement.setExpiry(ORDER_EXPIRY_TICKS);
      logisticsManager.registerOrder(getHome().getBuilding(), bottlesRequirement);

      ImportUpTo bottlesImport =
            new ImportUpTo(
                  level,
                  "glass_bottle",
                  bottlesRequirement.getItemSearch(),
                  StorehouseOrder.Origin.AUTOMATIC,
                  1,
                  MAX_BOTTLES,
                  MAX_BOTTLES);
      bottlesImport.setExpiry(ORDER_EXPIRY_TICKS);
      logisticsManager.registerOrder(getHome().getBuilding(), bottlesImport);
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), 5);
      hasWorkOutputItems = false;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);

      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      if (hasWorkOutputItems)
         getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_WORK_OUTPUT_AT_HOME);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      if (tickTime - lastWorkTime < WORK_INTERVAL_TICKS)
         return;

      lastWorkTime = tickTime;

      findFullBeehive(level);
      if (nextFullBeehive == null) {
         doStop(level, villager, tickTime);
         return;
      }

      Optional<ItemStack> shears = findShears(villager);
      Optional<ContainerHelper.ItemSearchResult> bottle = findGlassBottle(villager);

      if (shears.isEmpty() && bottle.isEmpty()) {
         doStop(level, villager, tickTime);
         return;
      }

      // if villager has both bottles and shears, it's a 50-50 chance to harvest honey or honeycomb, otherwise only the
      // available tool is used to harvest
      boolean harvestHoney = bottle.isPresent() && (shears.isEmpty() || villager.getRandom().nextBoolean());

      villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(nextFullBeehive, 0.25f, 2));
      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(nextFullBeehive));

      if (harvestHoney)
         fillBottleWithHoney(level, villager, bottle.get());
      else
         shearHoneycomb(level, villager, shears.get());

      emptyBeehive(level, nextFullBeehive);
      hasWorkOutputItems = true;
   }

   private void fillBottleWithHoney(
         ServerLevel level,
         CivilizedVillager villager,
         ContainerHelper.ItemSearchResult bottle) {
      villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GLASS_BOTTLE));
      villager.swing(InteractionHand.MAIN_HAND, true);

      villager.getWorkInputInventory().removeItem(bottle.slot(), 1);
      villager.getWorkOutputInventory().addItem(new ItemStack(Items.HONEY_BOTTLE));
      level.playSound(null, nextFullBeehive, SoundEvents.BOTTLE_FILL, villager.getSoundSource(), 1.0F, 1.0F);
   }

   private void shearHoneycomb(ServerLevel level, CivilizedVillager villager, ItemStack shears) {
      villager.setItemSlot(EquipmentSlot.MAINHAND, shears);
      villager.swing(InteractionHand.MAIN_HAND, true);

      villager.getWorkOutputInventory().addItem(new ItemStack(Items.HONEYCOMB, HONEYCOMB_PER_HIVE));
      level.playSound(null, nextFullBeehive, SoundEvents.BEEHIVE_SHEAR, villager.getSoundSource(), 1.0F, 1.0F);
   }

   private void emptyBeehive(ServerLevel level, BlockPos beehive) {
      BlockState state = level.getBlockState(beehive);
      level.setBlockAndUpdate(beehive, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
   }

   private Optional<ItemStack> findShears(CivilizedVillager villager) {
      return ContainerHelper.findItem(villager.getWorkInputInventory(), i -> i.getItem() instanceof ShearsItem)
            .map(ContainerHelper.ItemSearchResult::itemStack);
   }

   private Optional<ContainerHelper.ItemSearchResult> findGlassBottle(CivilizedVillager villager) {
      return ContainerHelper.findItem(villager.getWorkInputInventory(), i -> i.is(Items.GLASS_BOTTLE));
   }

   private void findFullBeehive(ServerLevel level) {
      nextFullBeehive = null;

      getWorksite().getBuilding().getBounds().traverseBlocksWithin(traversal -> {
         BlockPos pos = traversal.getCurrentBlockPos();
         if (!isFullBeehive(pos, level))
            return;

         nextFullBeehive = pos.immutable();
         traversal.terminate();
      });
   }

   private boolean isFullBeehive(BlockPos blockPos, ServerLevel level) {
      BlockState state = level.getBlockState(blockPos);
      return state.getBlock() instanceof BeehiveBlock
            && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS;
   }
}
