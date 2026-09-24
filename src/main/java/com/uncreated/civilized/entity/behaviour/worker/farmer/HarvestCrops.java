package com.uncreated.civilized.entity.behaviour.worker.farmer;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.logistics.hauling.ReservationKey;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TakeToInventoryInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ToolRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HarvestCrops extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private @Nullable BlockPos nextFarmland = null;
   private @Nullable BlockPos nextMaturesCropToHarvest = null;
   private MediumDistanceTravelTask travelHelper;
   private ItemStack handHeld;

   private static final ToolRequirement HOE_REQUIREMENT =
         new ToolRequirement("hoe", i -> i.getItem() instanceof HoeItem);

   public HarvestCrops() {
      super(WorkStates.HARVESTING_CROPS, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      InventoryStockRequirement.StockResult carrying = HOE_REQUIREMENT.evaluate(villager);
      if (!carrying.satisfied()) {
         String party = reservationPartyKey(villager);

         Optional<TakeToInventoryInstruction> instruction =
               TakeToInventoryInstruction.createIfMetFromSourceBuildings(
                     new ReservationKey(party, HOE_REQUIREMENT.key()),
                     HOE_REQUIREMENT,
                     homeAndStorehouseIfPresent());
         if (instruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), instruction.get());
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            getStateMachine().queueActionOnce(this.getState());
            // todo: send notification that the villager is missing a tool
         }
         return false;
      }

      this.handHeld = carrying.stock().getItemStacks().getFirst();

      findFarmBlocks(level);
      return nextMaturesCropToHarvest != null;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), 5);
      hasWorkOutputItems = false;
      villager.setItemSlot(EquipmentSlot.MAINHAND, handHeld);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);

      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      if (hasWorkOutputItems)
         goDropOffWorkOutputAtHome(villager);

   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager entity, long gameTime) {
      super.canStillUse(level, entity, gameTime);
      Optional<GlobalPos> optional = entity.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      if (optional.isEmpty()) {
         return false;
      }

      return true;
   }

   private int toolHits = 0;
   boolean hasWorkOutputItems;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      if (tickTime - lastWorkTime > 25) {

         lastWorkTime = tickTime;

         findFarmBlocks(level);

         if (nextMaturesCropToHarvest == null) {
            doStop(level, villager, tickTime);
            return;
         }

         BlockState cropState = level.getBlockState(nextMaturesCropToHarvest);
         if (!(cropState.getBlock() instanceof CropBlock cropBlock))
            return;

         villager.getBrain()
               .setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(nextMaturesCropToHarvest, 0.25f, 1));
         villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(nextMaturesCropToHarvest));

         toolHits++;
         villager.swing(InteractionHand.MAIN_HAND, true);
         villager.addWorkExhaustion(1);

         if (toolHits == 4) {
            List<ItemStack> drops =
                  getDrops(level.getBlockState(nextMaturesCropToHarvest), level, nextMaturesCropToHarvest);
            drops.forEach(i -> villager.getInventory().addItem(i));

            level.setBlockAndUpdate(nextMaturesCropToHarvest, getCropReplantState(cropState, cropBlock));
            villager.playSound(SoundEvents.CROP_BREAK, 1.0f, 1.0f);
            toolHits = 0;

            hasWorkOutputItems = true;
         }
      }
   }

   private BlockState getCropReplantState(BlockState currentState, CropBlock cropBlock) {
      return currentState.setValue(CropBlock.AGE, cropBlock.getStateForAge(0).getValue(CropBlock.AGE));
   }

   private List<ItemStack> getDrops(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos) {
      Item replant = blockState.getCloneItemStack(serverLevel, blockPos, true).getItem();
      final boolean[] removedReplant = { false };

      List<ItemStack> drops = Block.getDrops(blockState, serverLevel, blockPos, null);
      drops.forEach(stack -> {
         if (!removedReplant[0] && stack.getItem() == replant) {
            stack.setCount(stack.getCount() - 1);
            removedReplant[0] = true;
         }
      });
      return drops;
   }

   private void findFarmBlocks(ServerLevel serverLevel) {
      nextFarmland = null;
      nextMaturesCropToHarvest = null;

      getWorksite().getBuilding().getBounds().traverseBlocksWithinTerminateYChecksIfCanSeeSky(b -> {

         if (isFarmland(b, serverLevel)) {
            nextFarmland = b.immutable();
         }
         BlockPos above = b.above().immutable();
         if (isMatureCrop(above, serverLevel)) {
            nextMaturesCropToHarvest = above;
         }
      }, serverLevel);
   }

   private boolean isFarmland(BlockPos blockPos, ServerLevel serverLevel) {
      BlockState farmland = serverLevel.getBlockState(blockPos);
      BlockState above = serverLevel.getBlockState(blockPos.above());
      return farmland.getBlock() instanceof FarmBlock && above.isAir();
   }

   private boolean isMatureCrop(BlockPos blockPos, ServerLevel serverLevel) {
      BlockState crop = serverLevel.getBlockState(blockPos);
      return crop.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(crop);
   }
}
