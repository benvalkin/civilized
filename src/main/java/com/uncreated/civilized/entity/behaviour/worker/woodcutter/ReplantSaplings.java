package com.uncreated.civilized.entity.behaviour.worker.woodcutter;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.orders.StorehouseOrder;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportUpTo;
import com.uncreated.civilized.core.building.logistics.orders.task.TaskConsumableItemRequirement;
import com.uncreated.civilized.core.building.logistics.orders.task.TaskItemRequirement;
import com.uncreated.civilized.core.building.state.GroveState;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ReplantSaplings extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private final List<BlockPos> validPlantingBlocks = Lists.newArrayList();
   private MediumDistanceTravelTask travelHelper;
   private Predicate<ItemStack> saplingFilter;

   public ReplantSaplings() {
      super(WorkStates.REPLANT_SAPLINGS, true, true, 60 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();

      GroveState behaviour = (GroveState) getWorksite().getBuilding().getState();
      TaskItemRequirement taskItemRequirement =
            new TaskConsumableItemRequirement(
                  level,
                  "replant_saplings",
                  behaviour::isCorrectSapling,
                  StorehouseOrder.Origin.AUTOMATIC,
                  16);
      taskItemRequirement.setExpiry(12000);
      logisticsManager.registerOrder(getHome().getBuilding(), taskItemRequirement);
      ImportUpTo importOrder =
            new ImportUpTo(
                  level,
                  "saplings",
                  taskItemRequirement.getItemSearch(),
                  StorehouseOrder.Origin.AUTOMATIC,
                  1,
                  8,
                  32);
      importOrder.setExpiry(12000);
      logisticsManager.registerOrder(getHome().getBuilding(), importOrder);

      saplingFilter = taskItemRequirement.getItemSearch();

      if (getSaplingsInInventory(villager).isEmpty()) {
         // todo: send notification that the villager is missing tool
         getStateMachine().queueActionOnce(WorkStates.FETCHING_WORK_INPUT_FROM_HOME);
         getStateMachine().queueActionOnce(this.getState());
         return false;
      }

      findValidPlantingBlocks(level);
      return !validPlantingBlocks.isEmpty(); // only start when it is possible to replant saplings
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), 8);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {

      if (entity.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
         return false;
      } else if (validPlantingBlocks.isEmpty()) {
         return false;
      }

      return true;
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(gameTime);
         return;
      }

      if (gameTime - lastWorkTime > 15) {

         lastWorkTime = gameTime;

         findValidPlantingBlocks(level);

         Optional<BlockPos> blockPos = validPlantingBlocks.stream().findFirst();
         if (blockPos.isEmpty())
            return;

         BlockPos pos = blockPos.get();

         villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, 0.25f, 3));
         villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));

         Optional<ItemStack> saplingStack = getSaplingsInInventory(villager);
         if (saplingStack.isEmpty()) {
            doStop(level, villager, gameTime);
            return;
         }

         villager.swing(InteractionHand.MAIN_HAND, true);

         SaplingBlock saplingBlock = (SaplingBlock) Block.byItem(saplingStack.get().getItem());

         level.setBlockAndUpdate(pos, saplingBlock.defaultBlockState());
         villager.playSound(SoundEvents.CROP_BREAK, 1.0f, 1.0f);

         villager.getInventory().removeItemType(saplingStack.get().getItem(), 1);
      }
   }

   private void findValidPlantingBlocks(ServerLevel serverLevel) {

      Building worksite = getWorksite().getBuilding();
      BlockPos.MutableBlockPos current = worksite.getBlockPos().mutable();
      validPlantingBlocks.clear();

      BlockPos lowerCorner = worksite.getBounds().getLowerCorner();
      BlockPos upperCorner = worksite.getBounds().getUpperCorner();

      int plantingXZMargin = 3;
      for (int x = lowerCorner.getX() + plantingXZMargin; x <= upperCorner.getX() - plantingXZMargin; x++) {
         for (int z = lowerCorner.getZ() + plantingXZMargin; z <= upperCorner.getZ() - plantingXZMargin; z++) {
            for (int y = lowerCorner.getY(); y <= upperCorner.getY(); y++) {
               current.set(x, y, z);

               BlockState currentState = serverLevel.getBlockState(current);
               if (isValidPlantingBlock(currentState)) {
                  BlockPos above = current.move(Direction.UP);
                  BlockState aboveState = serverLevel.getBlockState(above);
                  if (!aboveState.isEmpty())
                     continue;

                  validPlantingBlocks.add(above);
                  return;
               }
            }
         }
      }
   }

   private boolean isValidPlantingBlock(BlockState blockState) {
      return blockState.getTags().anyMatch(t -> t.equals(BlockTags.DIRT));
   }

   private Optional<ItemStack> getSaplingsInInventory(CivilizedVillager villager) {
      return villager.getWorkInputInventory().getItems().stream().filter(saplingFilter).findFirst();
   }
}
