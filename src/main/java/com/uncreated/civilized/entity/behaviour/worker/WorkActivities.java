package com.uncreated.civilized.entity.behaviour.worker;

import static com.uncreated.civilized.entity.behaviour.CivilizedVillagerActivities.getMinimalLookBehavior;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.BehaviourStates;
import com.uncreated.civilized.entity.behaviour.IdleStrollAroundSettlement;
import com.uncreated.civilized.entity.behaviour.UpdateActivityFromSchedule;
import com.uncreated.civilized.entity.behaviour.worker.artisan.CraftItems;
import com.uncreated.civilized.entity.behaviour.worker.artisan.furnace.SmeltItems;
import com.uncreated.civilized.entity.behaviour.worker.common.IdleStrollAroundWorksite;
import com.uncreated.civilized.entity.behaviour.worker.common.IdleStrollOutsideWorksite;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.CheckLogisticsOpportunities;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.DropOffExportsAtStorehouse;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.DropOffImportsAtHome;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.DropoffWorkOutputAtHome;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.FetchExportsFromHome;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.FetchImportsFromStorehouse;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.FetchWorkInputFromHome;
import com.uncreated.civilized.entity.behaviour.worker.farmer.HarvestCrops;
import com.uncreated.civilized.entity.behaviour.worker.farmer.PlantCrops;
import com.uncreated.civilized.entity.behaviour.worker.miner.MineOres;
import com.uncreated.civilized.entity.behaviour.worker.rancher.BreedAnimals;
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
                              new DropoffWorkOutputAtHome(),
                              new FetchExportsFromHome(),
                              new DropOffExportsAtStorehouse(),
                              new FetchImportsFromStorehouse(),
                              new DropOffImportsAtHome(),
                              new FetchWorkInputFromHome(),
                              new IdleStrollAroundWorksite(5, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(WorkStates.HARVESTING_CROPS, WorkStates.PLANTING_CROPS),
                        ImmutableList
                              .of(WorkStates.STROLL_AROUND_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))),
            Pair.of(99, UpdateActivityFromSchedule.create()));
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
                              new DropoffWorkOutputAtHome(),
                              new FetchExportsFromHome(),
                              new DropOffExportsAtStorehouse(),
                              new FetchImportsFromStorehouse(),
                              new DropOffImportsAtHome(),
                              new FetchWorkInputFromHome(),
                              new IdleStrollAroundWorksite(5, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(
                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.CUTTING_DOWN_TREES,
                              WorkStates.REPLANT_SAPLINGS),
                        ImmutableList
                              .of(WorkStates.STROLL_AROUND_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))),
            Pair.of(99, UpdateActivityFromSchedule.create()));
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
                              new DropoffWorkOutputAtHome(),
                              new FetchExportsFromHome(),
                              new DropOffExportsAtStorehouse(),
                              new FetchImportsFromStorehouse(),
                              new DropOffImportsAtHome(),
                              new FetchWorkInputFromHome(),
                              new IdleStrollAroundWorksite(5, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(WorkStates.MINING_ORES, WorkStates.CHECK_LOGISTICS_OPPORTUNITIES),
                        ImmutableList
                              .of(WorkStates.STROLL_AROUND_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))),
            Pair.of(99, UpdateActivityFromSchedule.create()));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<CivilizedVillager>>> getRancherWorkPackage() {
      return ImmutableList.of(
            getMinimalLookBehavior(),
            Pair.of(
                  1,
                  new WorkBehaviourControl(
                        ImmutableList.of(
                              new BreedAnimals<>(),
                              new SlaughterAnimals<>(Cow.class),
                              new CheckLogisticsOpportunities(),
                              new DropoffWorkOutputAtHome(),
                              new FetchExportsFromHome(),
                              new DropOffExportsAtStorehouse(),
                              new FetchImportsFromStorehouse(),
                              new DropOffImportsAtHome(),
                              new FetchWorkInputFromHome(),
                              new IdleStrollOutsideWorksite(4, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(
                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.BREEDING_ANIMALS,
                              WorkStates.SLAUGHTERING_ANIMALS),
                        ImmutableList
                              .of(WorkStates.STROLL_OUTSIDE_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))),
            Pair.of(99, UpdateActivityFromSchedule.create()));
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
                              new DropoffWorkOutputAtHome(),
                              new FetchExportsFromHome(),
                              new DropOffExportsAtStorehouse(),
                              new FetchImportsFromStorehouse(),
                              new DropOffImportsAtHome(),
                              new IdleStrollAroundWorksite(4, 3, 0.25f),
                              new IdleStrollAroundSettlement(5, 3, 0.25f)),
                        ImmutableList.of(

                              WorkStates.CHECK_LOGISTICS_OPPORTUNITIES,
                              WorkStates.SMELTING_ITEMS,
                              WorkStates.CRAFTING_ITEMS),
                        ImmutableList
                              .of(WorkStates.STROLL_AROUND_WORKSITE, BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))),
            Pair.of(99, UpdateActivityFromSchedule.create()));
   }
}
