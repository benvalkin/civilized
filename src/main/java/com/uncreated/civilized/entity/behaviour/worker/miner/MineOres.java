package com.uncreated.civilized.entity.behaviour.worker.miner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

public class MineOres extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private MediumDistanceTravelTask travelHelper;

   private int workSpeedMultiplier = 2;
   private float minerLuckChange = 0.2f; // percentage chance to successfully mine an ore
   private int minerMaxYield = 2; // max ore item yield per ore block mined

   private boolean foundOres = false;
   Map<BlockPos, Long> recentlyMinedBlocks;
   private ItemStack handHeld;

   private static final ToolRequirement PICKAXE_REQUIREMENT =
         new ToolRequirement("pickaxe", i -> i.getItem() instanceof PickaxeItem);

   public MineOres() {
      super(WorkStates.MINING_ORES, true, true, 90 * 20, 30 * 20);
      recentlyMinedBlocks = new HashMap<>();
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      InventoryStockRequirement.StockResult carrying = PICKAXE_REQUIREMENT.evaluate(villager);
      if (!carrying.satisfied()) {
         String party = reservationPartyKey(villager);
         Optional<TakeToInventoryInstruction> instruction =
               TakeToInventoryInstruction.createIfMetFromSourceBuildings(
                     new ReservationKey(party, PICKAXE_REQUIREMENT.key()),
                     PICKAXE_REQUIREMENT,
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

      return true; // todo: setting worksite can move to start method in all work tasks
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      foundOres = false;
      travelHelper = new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), 2);
      villager.setItemSlot(EquipmentSlot.MAINHAND, handHeld);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);

      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      if (foundOres)
         goDropOffWorkOutputAtHome(villager);
   }

   @Override
   protected boolean canStillUse(ServerLevel level, CivilizedVillager villager, long gameTime) {
      return villager.getBrain().checkMemory(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT);
   }

   private int applyWorkSpeedMultiplier(int requiredToolHits) {
      return requiredToolHits / workSpeedMultiplier;
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(gameTime);
         return;
      }

      if (gameTime - lastWorkTime > applyWorkSpeedMultiplier(30)) {

         lastWorkTime = gameTime;

         villager.swing(InteractionHand.MAIN_HAND, true);
         villager.addWorkExhaustion(1);

         mineOreVein(level, villager, gameTime);
      }
   }

   private void mineOreVein(ServerLevel level, CivilizedVillager villager, long gameTime) {
      int miningDepth = 0;

      LevelChunk chunk = level.getChunkAt(getWorksite().getBuilding().getBlockPos());
      int minX = chunk.getPos().getMinBlockX();
      int minZ = chunk.getPos().getMinBlockZ();
      int maxX = chunk.getPos().getMaxBlockX();
      int maxZ = chunk.getPos().getMaxBlockZ();
      int minY = Math.max(miningDepth, chunk.getMinY());
      int maxY = getWorksite().getBuilding().getBlockPos().getY();

      int mineX = villager.getRandom().nextInt(minX, maxX + 1);
      int mineY = villager.getRandom().nextInt(minY, maxY + 1);
      int mineZ = villager.getRandom().nextInt(minZ, maxZ + 1);

      BlockPos toMine = new BlockPos(mineX, mineY, mineZ);

      BlockState blockState = chunk.getBlockState(toMine);

      if (!(blockState.getBlock() instanceof DropExperienceBlock deb))
         return;

      Iterable<BlockPos> blocksToMine =
            BlockPos.betweenClosed(AABB.encapsulatingFullBlocks(toMine.offset(-1, -1, -1), toMine.offset(1, 1, 1)));
      blocksToMine.forEach(b -> mineBlock(b, villager, chunk, level, gameTime));

   }

   private void mineBlock(
         BlockPos toMine,
         CivilizedVillager villager,
         ChunkAccess chunk,
         ServerLevel level,
         long gameTime) {
      BlockState blockState = chunk.getBlockState(toMine);

      if (!(blockState.getBlock() instanceof DropExperienceBlock deb))
         return;

      Long timeLastMinedThisBlock = recentlyMinedBlocks.get(toMine);
      if (timeLastMinedThisBlock != null && gameTime - timeLastMinedThisBlock < 20 * 60 * 10)
         return;

      recentlyMinedBlocks.put(toMine, timeLastMinedThisBlock);

      List<ItemStack> drops = Block.getDrops(blockState, level, toMine, null);

      for (int i = 0; i < drops.size(); i++) {
         ItemStack drop = drops.get(i);
         if (i > 0 && villager.getRandom().nextFloat() > minerLuckChange)
            continue;

         int adjustedYield = Math.min(drop.getCount(), minerMaxYield);
         drop.setCount(adjustedYield);
         villager.getInventory().addItem(drop);
         foundOres = true;
      }
   }
}
