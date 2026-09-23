package com.uncreated.civilized.entity.behaviour.worker.fisherman;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.uncreated.civilized.core.building.logistics.hauling.ItemReservation;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TakeToInventoryInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ToolRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Fishes at a fishing spot. The villager casts its line at a water block inside the worksite, waits a while for
 * something to bite, and reels in whatever the vanilla fishing loot table gives it.
 */
public class Fish extends WorkTaskBehaviour {

   private static final int FISH_TO_CATCH_BEFORE_STOPPING = 5;
   private static final int ORDER_EXPIRY_TICKS = 12000;
   /** How far the villager will cast its line from wherever it is standing. */
   private static final int CAST_RANGE = 8;

   private final int minCatchTicks;
   private final int maxCatchTicks;

   private MediumDistanceTravelTask travelHelper;
   private ItemStack fishingRod;
   private @Nullable BlockPos fishingStand;
   private @Nullable BlockPos castTarget;
   private long nextCatchTime;
   private int fishCaught;

   private static final ToolRequirement fishingRodRequirement =
         new ToolRequirement("fishingRod", i -> i.is(Items.FISHING_ROD));

   /**
    * @param minCatchTicks
    *           shortest wait between casting the line and catching something
    * @param maxCatchTicks
    *           longest wait between casting the line and catching something
    */
   public Fish(int minCatchTicks, int maxCatchTicks) {
      super(WorkStates.FISHING, true, true, 120 * 20, 30 * 20);
      this.minCatchTicks = minCatchTicks;
      this.maxCatchTicks = maxCatchTicks;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      // the line needs somewhere to land, even though the building requirements already asked for a body of water
      List<BlockPos> waterBlocks = findWaterSurfaceBlocks(level);
      if (waterBlocks.isEmpty())
         return false;

      InventoryStockRequirement.StockResult carrying = fishingRodRequirement.evaluate(villager);
      if (!carrying.satisfied()) {
         String party = ItemReservation.partKeyFor(villager, this.getState().toString());
         Optional<TakeToInventoryInstruction> instruction =
               TakeToInventoryInstruction.createIfMetFromSourceBuildings(
                     party,
                     fishingRodRequirement.key(),
                     fishingRodRequirement,
                     homeAndStorehouseIfPresent());
         if (instruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), instruction.get());
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            getStateMachine().queueActionOnce(this.getState());
            // todo: send notification that the villager is missing shears
         }
         return false;
      }

      fishingRod = carrying.stock().getItemStacks().getFirst();

      // fishing from dry land looks a lot better than wading in, but a spot that is all water still gets fished from
      fishingStand = findFishingStand(level, villager, waterBlocks).orElse(getWorksite().getBuilding().getBlockPos());

      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, fishingStand, 1);
      villager.setItemSlot(EquipmentSlot.MAINHAND, fishingRod);
      castTarget = null;
      fishCaught = 0;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);

      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      if (fishCaught > 0)
         goDropOffWorkOutputAtHome(villager);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      if (castTarget == null || !isWaterSurface(level, castTarget)) {
         castLine(level, villager, tickTime);
         return;
      }

      if (tickTime < nextCatchTime)
         return;

      catchFish(level, villager);

      if (fishCaught >= FISH_TO_CATCH_BEFORE_STOPPING) {
         doStop(level, villager, tickTime);
         return;
      }

      castLine(level, villager, tickTime);
   }

   private void castLine(ServerLevel level, CivilizedVillager villager, long tickTime) {
      List<BlockPos> waterBlocks = findWaterSurfaceBlocks(level);
      if (waterBlocks.isEmpty()) {
         doStop(level, villager, tickTime);
         return;
      }

      List<BlockPos> waterInRange =
            waterBlocks.stream().filter(w -> w.closerToCenterThan(villager.position(), CAST_RANGE)).toList();
      if (!waterInRange.isEmpty())
         waterBlocks = waterInRange;

      castTarget = waterBlocks.get(villager.getRandom().nextInt(waterBlocks.size()));
      nextCatchTime = tickTime + villager.getRandom().nextInt(minCatchTicks, maxCatchTicks + 1);

      villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(castTarget));
      villager.swing(InteractionHand.MAIN_HAND, true);
      level.playSound(null, castTarget, SoundEvents.FISHING_BOBBER_THROW, villager.getSoundSource(), 1.0F, 1.0F);
   }

   private void catchFish(ServerLevel level, CivilizedVillager villager) {

      int luck = EnchantmentHelper.getFishingLuckBonus(level, fishingRod, villager);

      LootParams lootParams =
            new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, castTarget.getCenter())
                  .withParameter(LootContextParams.TOOL, fishingRod)
                  .withParameter(LootContextParams.ATTACKING_ENTITY, villager)
                  .withLuck(luck)
                  .create(LootContextParamSets.FISHING);
      LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
      List<ItemStack> caughtItems = lootTable.getRandomItems(lootParams);

      level.playSound(null, castTarget, SoundEvents.FISHING_BOBBER_SPLASH, villager.getSoundSource(), 1.0F, 1.0F);
      villager.swing(InteractionHand.MAIN_HAND, true);

      if (caughtItems.isEmpty())
         return;

      caughtItems.forEach(i -> villager.getWorkOutputInventory().addItem(i));
      fishCaught++;
      level.playSound(null, castTarget, SoundEvents.FISHING_BOBBER_RETRIEVE, villager.getSoundSource(), 1.0F, 1.0F);
   }

   private List<BlockPos> findWaterSurfaceBlocks(ServerLevel level) {
      List<BlockPos> waterBlocks = Lists.newArrayList();

      getWorksite().getBuilding().getBounds().traverseBlocksWithin(traversal -> {
         BlockPos pos = traversal.getCurrentBlockPos();
         if (isWaterSurface(level, pos))
            waterBlocks.add(pos.immutable());
      });

      return waterBlocks;
   }

   private boolean isWaterSurface(ServerLevel level, BlockPos blockPos) {
      return level.getFluidState(blockPos).is(FluidTags.WATER) && level.getBlockState(blockPos.above()).isAir();
   }

   /**
    * Looks for dry land next to the water to fish from. Only positions beside a water block are considered, so the
    * villager always ends up on the shore, and of those it takes the one closest to itself.
    */
   private Optional<BlockPos> findFishingStand(
         ServerLevel level,
         CivilizedVillager villager,
         List<BlockPos> waterBlocks) {

      BlockPos closestStand = null;
      double closestDistance = Double.MAX_VALUE;

      for (BlockPos water : waterBlocks) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            // the shore can be level with the water or a block above it
            for (int y = 0; y <= 1; y++) {
               BlockPos candidate = water.relative(direction).above(y);

               if (!isStandable(level, candidate))
                  continue;

               double distance = candidate.distToCenterSqr(villager.position());
               if (distance >= closestDistance)
                  continue;

               closestDistance = distance;
               closestStand = candidate;
            }
         }
      }

      return Optional.ofNullable(closestStand);
   }

   private boolean isStandable(ServerLevel level, BlockPos blockPos) {
      BlockState below = level.getBlockState(blockPos.below());
      if (!below.blocksMotion() || !below.getFluidState().isEmpty())
         return false;

      return level.getBlockState(blockPos).isAir() && level.getBlockState(blockPos.above()).isAir();
   }
}
