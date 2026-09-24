package com.uncreated.civilized.entity.behaviour.worker;

import static com.uncreated.civilized.entity.behaviour.CivilizedVillagerActivities.getMinimalLookBehavior;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.IdleStrollAroundSettlement;
import com.uncreated.civilized.entity.behaviour.worker.artisan.CraftItems;
import com.uncreated.civilized.entity.behaviour.worker.artisan.furnace.SmeltItems;
import com.uncreated.civilized.entity.behaviour.worker.beekeeper.HarvestHoneyAndHoneyComb;
import com.uncreated.civilized.entity.behaviour.worker.common.MonitorWorksite;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.CheckLogisticsOpportunities;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.DropOffItemsAtBuilding;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.TakeItemsToInventory;
import com.uncreated.civilized.entity.behaviour.worker.farmer.HarvestCrops;
import com.uncreated.civilized.entity.behaviour.worker.farmer.PlantCrops;
import com.uncreated.civilized.entity.behaviour.worker.fisherman.Fish;
import com.uncreated.civilized.entity.behaviour.worker.miner.MineOres;
import com.uncreated.civilized.entity.behaviour.worker.rancher.BreedAnimals;
import com.uncreated.civilized.entity.behaviour.worker.rancher.ShearSheep;
import com.uncreated.civilized.entity.behaviour.worker.rancher.SlaughterAnimals;
import com.uncreated.civilized.entity.behaviour.worker.woodcutter.CutDownTrees;
import com.uncreated.civilized.entity.behaviour.worker.woodcutter.ReplantSaplings;

import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.animal.Cow;

public class WorkActivities {

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getWorkPackage(
         VillagerOccupation occupation) {

      if (occupation.workBehaviourPackage() == null)
         throw new IllegalStateException(
               String.format("Villager occupation '%s' does not support working behaviour.", occupation));

      return occupation.workBehaviourPackage().get();
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getFarmerWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new HarvestCrops(),
                              new PlantCrops(),
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(5, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(
                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.HARVESTING_CROPS,
                              WorkStates.PLANTING_CROPS),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getWoodcutterWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new CutDownTrees(),
                              new ReplantSaplings(),
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(5, 3, 0.25f, true),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(
                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.CUTTING_DOWN_TREES,
                              WorkStates.REPLANT_SAPLINGS),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getMinerWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new MineOres(),
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(5, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(WorkStates.MINING_ORES, WorkStates.CHECK_LOGISTICS_OPPORTUNITIES),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getRancherWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new BreedAnimals<>(),
                              new ShearSheep(),
                              new SlaughterAnimals<>(),
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(4, 3, 0.25f, true),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(
                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.BREEDING_ANIMALS,
                              WorkStates.SHEARING_SHEEP,
                              WorkStates.SLAUGHTERING_ANIMALS),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getBeekeeperWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new BreedAnimals<>(),
                              new HarvestHoneyAndHoneyComb(),
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(4, 3, 0.25f, true),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(
                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.BREEDING_ANIMALS,
                              WorkStates.HARVESTING_HONEY),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getFishermanWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new Fish(10 * 20, 30 * 20),
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(4, 3, 0.25f, true),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(WorkStates.CHECK_LOGISTICS_OPPORTUNITIES, WorkStates.FISHING),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getArtisanWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new SmeltItems(),
                              new CraftItems(),
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(4, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(
                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.SMELTING_ITEMS,
                              WorkStates.CRAFTING_ITEMS),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getGuardWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new CheckLogisticsOpportunities(),
                              new TakeItemsToInventory(),
                              new DropOffItemsAtBuilding(),
                              new MonitorWorksite(4, 3, 0.25f, true),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(WorkStates.CHECK_LOGISTICS_OPPORTUNITIES),
                        ImmutableList.of(WorkStates.MONITOR_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }
}
