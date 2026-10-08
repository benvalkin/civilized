package com.uncreated.civilized.entity.behaviour;

import java.util.Optional;
import java.util.function.Function;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StrollWithinBuildingBounds extends StatefulBehaviour {

   private final Function<CivilizedVillager, Optional<LoadedBuilding>> findBuilding;
   protected final BoundsStroller stroller;

   private LoadedBuilding building;

   public StrollWithinBuildingBounds(
         BehaviourState state,
         int minDuration,
         int maxDuration,
         int maxHorizontalDist,
         int maxVerticalDist,
         float speedModifier,
         Function<CivilizedVillager, Optional<LoadedBuilding>> findBuilding) {
      super(state, minDuration, maxDuration, 0);
      this.findBuilding = findBuilding;
      this.stroller = new BoundsStroller(BoundsStroller.Area.INSIDE, maxHorizontalDist, maxVerticalDist, speedModifier);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {
      Optional<LoadedBuilding> found = findBuilding.apply(villager);
      found.ifPresent(b -> building = b);
      return found.isPresent();
   }

   @Override
   protected void start(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.start(level, villager, gameTime);
      stroller.start(gameTime);
   }

   @Override
   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {
      stroller.tick(villager, building.getBuilding().getBounds(), gameTime);
   }

   @Override
   protected void stop(ServerLevel level, CivilizedVillager villager, long gameTime) {
      super.stop(level, villager, gameTime);
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }
}
