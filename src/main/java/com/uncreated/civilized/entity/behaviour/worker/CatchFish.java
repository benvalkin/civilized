package com.uncreated.civilized.entity.behaviour.worker;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.building.logistics.LogisticsManager;
import com.uncreated.civilized.core.building.logistics.orders.StorehouseOrder;
import com.uncreated.civilized.core.building.logistics.orders.imports.ImportUpTo;
import com.uncreated.civilized.core.building.logistics.orders.task.ToolRequirement;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.MediumDistanceTravelTask;
import com.uncreated.civilized.util.ContainerHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class CatchFish extends WorkTaskBehaviour {
   public static final Logger LOGGER = LogUtils.getLogger();
   private long nextWorkTime;
   private MediumDistanceTravelTask travelHelper;

   int workSpeedMultiplier = 1;
   private ItemStack handHeld;

   public CatchFish() {
      super(WorkStates.FISHING, true, true, 120 * 20, 30 * 20);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      LogisticsManager logisticsManager = getSettlement().getBehaviour().getLogisticsManager();

      ToolRequirement toolRequirement =
            new ToolRequirement(level, "catch_fish", FishingRodItem.class, StorehouseOrder.Origin.AUTOMATIC);
      toolRequirement.setExpiry(12000);
      logisticsManager.registerOrder(getWorksite().getBuilding(), toolRequirement);
      ImportUpTo importOrder =
            new ImportUpTo(
                  level,
                  "fishing_rod",
                  toolRequirement.getItemSearch(),
                  StorehouseOrder.Origin.AUTOMATIC,
                  1,
                  1,
                  1);
      importOrder.setExpiry(12000);
      logisticsManager.registerOrder(getHome().getBuilding(), importOrder);

      Optional<ContainerHelper.ItemSearchResult> tool =
            ContainerHelper.findItem(villager.getWorkInputInventory(), toolRequirement.getItemSearch());
      if (tool.isEmpty()) {
         // todo: send notification that the villager is missing tool
         getStateMachine().queueActionOnce(WorkStates.FETCHING_WORK_INPUT_FROM_HOME);
         getStateMachine().queueActionOnce(this.getState());
         return false;
      }
      this.handHeld = tool.get().itemStack();
      return true;
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      travelHelper = new MediumDistanceTravelTask(villager, getWorksite().getBuilding().getBlockPos(), 8);
      villager.setItemSlot(EquipmentSlot.MAINHAND, handHeld);
      hasFish = false;
      nextWorkTime = gameTime;
      setNextWorkTime();
   }

   private void setNextWorkTime() {
      nextWorkTime += new Random().nextInt(30 * 20, 60 * 20) / workSpeedMultiplier;
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      if (hasFish)
         getStateMachine().queueActionOnce(WorkStates.DROPPING_OFF_WORK_OUTPUT_AT_HOME);
   }

   private boolean hasFish;

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long tickTime) {

      if (!travelHelper.isJourneySuccessful()) {
         travelHelper.walkToPoi(tickTime);
         return;
      }

      if (nextWorkTime > tickTime) {

         setNextWorkTime();

         BlockPos fishingBlockPos = villager.getOnPos();

         int luck = EnchantmentHelper.getFishingLuckBonus(level, handHeld, villager);

         LootParams lootparams =
               (new LootParams.Builder(level)).withParameter(LootContextParams.ORIGIN, fishingBlockPos.getCenter())
                     .withParameter(LootContextParams.TOOL, handHeld)
                     .withParameter(LootContextParams.ATTACKING_ENTITY, villager)
                     .withLuck(luck/* + villagerLuck */)
                     .create(LootContextParamSets.FISHING);
         LootTable loottable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
         List<ItemStack> fishedItems = loottable.getRandomItems(lootparams);

         fishedItems.forEach(i -> villager.getWorkOutputInventory().addItem(i));

         if (!fishedItems.isEmpty())
            hasFish = true;
      }
   }
}
