package com.uncreated.civilized.entity.behaviour.worker.miner;

import java.util.ArrayDeque;
import java.util.Deque;
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
import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
import com.uncreated.civilized.util.ConnectedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class MineOres extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long lastWorkTime;
   private MediumDistanceTravelTask travelHelper;

   private int minerMaxYield = 2; // max ore item yield per ore block mined

   private boolean foundOres = false;
   private final Map<BlockPos, Long> recentlyMinedBlocks = new HashMap<>();
   private final Deque<BlockPos> vein = new ArrayDeque<>();
   private ItemStack handHeld;

   /** Ores from any mod */
   private static final TagKey<Block> ORES =
         TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
   /** The most blocks a vein search can search through. Prevents searches from taking too long. */
   private static final int MAX_VEIN_SIZE = 64;
   private static final long REMINE_COOLDOWN_TICKS = 20 * 60 * 10;
   private static final int MINING_DEPTH = 0;

   private static final ToolRequirement PICKAXE_REQUIREMENT =
         new ToolRequirement("pickaxe", i -> i.getItem() instanceof PickaxeItem);

   public MineOres() {
      super(WorkStates.MINING_ORES, true, true, 90 * 20, 30 * 20);
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
                     villager,
                     new ReservationKey(party, PICKAXE_REQUIREMENT.key()),
                     PICKAXE_REQUIREMENT,
                     homeAndStorehouseIfPresent());
         if (instruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), instruction.get());
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            getStateMachine().queueActionOnce(this.getState());
         } else {
            notifyMissingItem(
                  Notification.missingTool(
                        "missing_pickaxe",
                        villager.getInfo(),
                        Component.translatable("notification.worker.tool.pickaxe"),
                        new ItemStack(Items.IRON_PICKAXE)));
         }
         return false;
      }

      resolveMissingItemNotification();
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

   private int getRequiredToolHits() {
      int blocksPerDay = 28;
      return CivilizedVillager.WORK_DAY_DURATION_SECONDS / blocksPerDay;
   }

   private int toolHits = 0;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(gameTime);
         return;
      }

      if (gameTime - lastWorkTime > 20) {
         lastWorkTime = gameTime;
         toolHits++;

         if (toolHits >= getRequiredToolHits()) {
            toolHits = 0;

            mineNextOre(level, villager, gameTime);
         }

         villager.addWorkExhaustion(0.5f);
         villager.swing(InteractionHand.MAIN_HAND, true);
      }
   }

   private void mineNextOre(ServerLevel level, CivilizedVillager villager, long gameTime) {
      LevelChunk chunk = level.getChunkAt(getWorksite().getBuilding().getBlockPos());

      // mine existing vein
      while (!vein.isEmpty()) {
         BlockPos next = vein.poll();
         if (isOreAndMinable(level, next, gameTime)) {
            mineBlock(next, villager, level, gameTime);
            return;
         }
      }

      // otherwise, sample terrain, and hopefully we'll find a new vein

      // allow previously-mined blocks to be sampled again if it's been long enough
      recentlyMinedBlocks.values().removeIf(minedAt -> gameTime - minedAt >= REMINE_COOLDOWN_TICKS);

      int minY = Math.max(MINING_DEPTH, chunk.getMinY());
      int maxY = getWorksite().getBuilding().getBlockPos().getY();
      ChunkPos chunkPos = chunk.getPos();
      BlockPos sample =
            new BlockPos(
                  villager.getRandom().nextInt(chunkPos.getMinBlockX(), chunkPos.getMaxBlockX() + 1),
                  villager.getRandom().nextInt(minY, maxY + 1),
                  villager.getRandom().nextInt(chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ() + 1));

      if (!isOreAndMinable(level, sample, gameTime)) {
         // mine stone or whatever else - hopefully we'll find an ore vein next work tick
         if (isStoneBlock(level, sample))
            mineBlock(sample, villager, level, gameTime);
         return;
      }

      // find all blocks in the ore vein. Searched through the level rather than the chunk, so veins that cross into
      // neighbouring chunks are followed (a chunk wraps positions outside of it back onto its own blocks)
      List<BlockPos> connectedBlocks =
            ConnectedBlocks.findConnectedBlocks(
                  level,
                  sample,
                  ConnectedBlocks.Adjacency.ALL,
                  MAX_VEIN_SIZE,
                  (pos, state) -> isOreAndMinable(level, pos, gameTime));
      vein.addAll(connectedBlocks);
      // mine the first block in the vein
      mineBlock(vein.poll(), villager, level, gameTime);
   }

   private boolean isOreAndMinable(ServerLevel level, BlockPos pos, long gameTime) {
      // reading a block in an unloaded chunk would load it, so veins stop at unloaded chunks
      if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ORES))
         return false;

      Long minedAt = recentlyMinedBlocks.get(pos);
      return minedAt == null || gameTime - minedAt >= REMINE_COOLDOWN_TICKS;
   }

   private boolean isStoneBlock(ServerLevel level, BlockPos pos) {
      return level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD);
   }

   private void mineBlock(BlockPos toMine, CivilizedVillager villager, ServerLevel level, long gameTime) {
      BlockState blockState = level.getBlockState(toMine);
      recentlyMinedBlocks.put(toMine, gameTime);

      for (ItemStack drop : Block.getDrops(blockState, level, toMine, null)) {
         drop.setCount(Math.min(drop.getCount(), minerMaxYield));
         villager.getInventory().addItem(drop);
         foundOres = true;
      }
   }
}
