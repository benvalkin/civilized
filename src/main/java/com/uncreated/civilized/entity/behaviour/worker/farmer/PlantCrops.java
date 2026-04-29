package com.uncreated.civilized.entity.behaviour.worker.farmer;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.orders.StorehouseOrder;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportUpTo;
import com.uncreated.civilized.core.building.logistics.orders.task.TaskConsumableItemRequirement;
import com.uncreated.civilized.core.building.logistics.orders.task.TaskItemRequirement;
import com.uncreated.civilized.core.building.state.CropFarmState;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PlantCrops extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private final List<BlockPos> emptyFarmland = Lists.newArrayList();
   private MediumDistanceTravelTask travelHelper;

   public PlantCrops() {
      super(WorkStates.PLANTING_CROPS, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      findFarmland(level);

      if (emptyFarmland.isEmpty())
         return false;

      LogisticsManager logisticsManager =getSettlement().getBehaviour().getLogisticsManager();
      CropFarmState behaviour = (CropFarmState) getWorksite().getBuilding().getState();
      TaskItemRequirement taskItemRequirement =
            new TaskConsumableItemRequirement(
                  level,
                  "plant_crops",
                  behaviour::isCorrectCrop,
                  StorehouseOrder.Origin.AUTOMATIC,
                  16);
      taskItemRequirement.setExpiry(12000);
      logisticsManager.registerOrder(getHome().getBuilding(), taskItemRequirement);
      ImportUpTo importOrder =
            new ImportUpTo(
                  level,
                  "seeds",
                  taskItemRequirement.getItemSearch(),
                  StorehouseOrder.Origin.AUTOMATIC,
                  1,
                  16,
                  32);
      importOrder.setExpiry(12000);
      logisticsManager.registerOrder(getHome().getBuilding(), importOrder);

      if (!(hasSeedsInInventory(villager.getWorkInputInventory())
            || hasSeedsInInventory(villager.getWorkOutputInventory())))
         return false;

      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), 5);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager entity, long gameTime) {
      super.stop(level, entity, gameTime);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {

      Optional<GlobalPos> optional = entity.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      if (optional.isEmpty()) {
         return false;
      } else if (emptyFarmland.isEmpty()) {
         return false;
      }

      return true;
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      if (tickTime - lastWorkTime > 25) {

         lastWorkTime = tickTime;

         findFarmland(level);

         if (!emptyFarmland.isEmpty()) {
            BlockPos cropPos = emptyFarmland.getFirst();

            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(cropPos, 0.25f, 1));
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(cropPos));

            Optional<CropBlock> nextSeedToPlant =
                  takeNextSeedToPlant(villager.getWorkInputInventory())
                        .or(() -> takeNextSeedToPlant(villager.getWorkOutputInventory()));

            if (nextSeedToPlant.isEmpty())
               return;

            level.setBlockAndUpdate(cropPos, getCropReplantState(level.getBlockState(cropPos), nextSeedToPlant.get()));
            villager.playSound(SoundEvents.CROP_BREAK, 1.0f, 1.0f);
         }
      }
   }

   private Optional<CropBlock> takeNextSeedToPlant(Container sourceInventory) {
      for (int i = 0; i < sourceInventory.getContainerSize(); i++) {

         ItemStack itemStack = sourceInventory.getItem(i);
         if (!(itemStack.getItem() instanceof BlockItem blockItem))
            continue;

         if (!(blockItem.getBlock() instanceof CropBlock cropBlock))
            continue;

         itemStack.shrink(1);
         sourceInventory.setItem(i, itemStack);
         return Optional.of(cropBlock);
      }

      return Optional.empty();
   }

   private boolean hasSeedsInInventory(Container sourceInventory) {
      for (int i = 0; i < sourceInventory.getContainerSize(); i++) {

         ItemStack itemStack = sourceInventory.getItem(i);
         if (!(itemStack.getItem() instanceof BlockItem blockItem))
            continue;

         if (!(blockItem.getBlock() instanceof CropBlock))
            continue;

         return true;
      }

      return false;
   }

   private BlockState getCropReplantState(BlockState currentState, CropBlock cropBlock) {
      return currentState.setValue(CropBlock.AGE, cropBlock.getStateForAge(0).getValue(CropBlock.AGE));
   }

   private void findFarmland(ServerLevel serverLevel) {
      emptyFarmland.clear();

      getWorksite().getBuilding().getBounds().traverseBlocksWithin(traversal -> {
         BlockPos b = traversal.getCurrentBlockPos();
         if (isEmptyFarmland(b.below(), serverLevel)) {
            emptyFarmland.add(b);
         }
         if (serverLevel.canSeeSky(b))
            traversal.skipToNextXZ();
      });
   }

   private boolean isEmptyFarmland(BlockPos blockPos, ServerLevel serverLevel) {
      BlockState farmland = serverLevel.getBlockState(blockPos);
      BlockState above = serverLevel.getBlockState(blockPos.above());
      return farmland.getBlock() instanceof FarmBlock && above.isEmpty();
   }
}
