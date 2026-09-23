package com.uncreated.civilized.entity.behaviour.worker.beekeeper;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.logistics.hauling.VillagerInventoryType;
import com.uncreated.civilized.core.building.logistics.hauling.instruction.TakeToInventoryInstruction;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.InventoryStockRequirement;
import com.uncreated.civilized.core.building.logistics.hauling.requirement.ToolRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.entity.behaviour.worker.WorkStates;
import com.uncreated.civilized.entity.behaviour.worker.WorkTaskBehaviour;
import com.uncreated.civilized.neoforge.registration.ai.AIRegistry;
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
   private static final int WORK_INTERVAL_TICKS = 2 * 20;

   private MediumDistanceTravelTask travelHelper;
   private long lastWorkTime;
   private @Nullable BlockPos nextFullBeehive;
   private boolean hasWorkOutputItems;

   private static final ToolRequirement shearsRequirement = new ToolRequirement("shears", i -> i.is(Items.SHEARS));

   private static final InventoryStockRequirement glassBottlesRequirement =
         new InventoryStockRequirement(
               "glass_bottles",
               i -> i.is(Items.GLASS_BOTTLE),
               1,
               4,
               VillagerInventoryType.WORK_TASK);

   public HarvestHoneyAndHoneyComb() {
      super(WorkStates.HARVESTING_HONEY, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      if (!super.checkExtraStartConditions(level, villager))
         return false;

      findFullBeehive(level);
      if (nextFullBeehive == null)
         return false;

      InventoryStockRequirement.StockResult carryingShears = shearsRequirement.evaluate(villager);
      InventoryStockRequirement.StockResult carryingGlassBottles = glassBottlesRequirement.evaluate(villager);

      if (!carryingShears.satisfied() && !carryingGlassBottles.satisfied()) {

         String reservationKey = villager.getInfo().getVillagerId().toString();

         Optional<TakeToInventoryInstruction> instruction =
               TakeToInventoryInstruction.createIfAnyMetFromSourceBuildings(
                     reservationKey,
                     List.of(shearsRequirement, glassBottlesRequirement),
                     homeAndStorehouseIfPresent());
         if (instruction.isPresent()) {
            villager.getBrain().setMemory(AIRegistry.MM_TAKE_ITEMS_INSTRUCTION.get(), instruction.get());
            getStateMachine().queueActionOnce(WorkStates.TAKING_ITEMS_TO_INVENTORY);
            getStateMachine().queueActionOnce(this.getState());
            // todo: send notification that the villager is missing shears
         }
         return false;
      }

      return true;
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
         goDropOffWorkOutputAtHome(villager);
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
