package com.uncreated.civilized.entity.behaviour;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.worker.common.logistics.TakeItemsToInventory;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.WakeUp;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class CivilizedVillagerActivities {

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CivilizedVillager>>> getRestPackage(
         float speedModifier) {
      return ImmutableList.of(
            Pair.of(2, new SleepInBed(speedModifier)),
            // Pair.of(3, ValidateNearbyPoi.create((p_217495_) -> p_217495_.is(PoiTypes.HOME), MemoryModuleType.HOME)),
            Pair.of(
                  5,
                  new RunOne( // for now, do nothing at home
                        ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT),
                        ImmutableList.of(
                              // Pair.of(SetClosestHomeAsWalkTarget.create(speedModifier), 1),
                              // Pair.of(InsideBrownianWalk.create(speedModifier), 4),
                              // Pair.of(GoToClosestVillage.create(speedModifier, 4), 2),
                              Pair.of(new DoNothing(20, 40), 2)))),
            getMinimalLookBehavior());
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CivilizedVillager>>> getCorePackage(
         float speedModifier) {
      return ImmutableList.of(
            Pair.of(0, new MoveToTargetSink()),
            Pair.of(0, new Swim(0.8F)),
            Pair.of(0, InteractWithDoorAndGate.create()),
            Pair.of(0, new LookAtTargetSink(45, 90)),
            Pair.of(0, new InvalidateImportantLocations()),
            // wakes up villagers that are still asleep outside of rest time, e.g. after being loaded in asleep
            Pair.of(0, WakeUp.create()));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CivilizedVillager>>> getIdlePackage(
         float speedModifier) {
      return ImmutableList.of(
            getFullLookBehavior(),
            Pair.of(
                  1,
                  new IdleBehaviourControl(
                        ImmutableList.of(
                              new EatFood(BehaviourStates.EATING_FOOD),
                              new TakeItemsToInventory(),
                              new IdleStrollAroundSettlement(5, 3, speedModifier)),
                        ImmutableList.of(BehaviourStates.EATING_FOOD),
                        ImmutableList.of(BehaviourStates.IDLE_STROLL_AROUND_SETTLEMENT))));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CivilizedVillager>>> getPanicPackage(
         float speedModifier) {
      // priority 2, so that Routed ticks after the previous activity's behaviours (priority 1) have stopped. Otherwise
      // their stop methods would erase the walk target Routed sets on its first tick
      return ImmutableList.of(Pair.of(2, new Routed(speedModifier)));
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CivilizedVillager>>> getSpeakToPlayerPackage() {
      return ImmutableList.of(Pair.of(0, new SpeakToPlayer()));
   }

   public static Pair<Integer, BehaviorControl<CivilizedVillager>> getMinimalLookBehavior() {
      return Pair.of(
            5,
            new RunOne(
                  ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityRegistry.CIVILIZED_VILLAGER.get(), 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(new DoNothing(30, 60), 8))));
   }

   public static Pair<Integer, BehaviorControl<CivilizedVillager>> getFullLookBehavior() {
      return Pair.of(
            5,
            new RunOne(
                  ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityType.CAT, 8.0F), 8),
                        Pair.of(SetEntityLookTarget.create(EntityRegistry.CIVILIZED_VILLAGER.get(), 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(MobCategory.CREATURE, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.WATER_CREATURE, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.AXOLOTLS, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.WATER_AMBIENT, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.MONSTER, 8.0F), 1),
                        Pair.of(new DoNothing(30, 60), 2))));
   }
}
